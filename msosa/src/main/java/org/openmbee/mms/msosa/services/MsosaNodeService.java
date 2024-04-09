package org.openmbee.mms.msosa.services;

import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.config.Privileges;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.core.security.MethodSecurityService;
import org.openmbee.mms.core.services.HierarchicalNodeService;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.crud.services.DefaultNodeService;
import org.openmbee.mms.json.ElementJson;
import org.openmbee.mms.json.MountJson;
import org.openmbee.mms.msosa.MsosaConstants;
import org.openmbee.mms.msosa.MsosaNodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("msosaNodeService")
public class MsosaNodeService extends DefaultNodeService implements HierarchicalNodeService {

    protected MsosaHelper msosaHelper;
    private MethodSecurityService mss;

    @Autowired
    public void setMsosaHelper(MsosaHelper msosaHelper) {
        this.msosaHelper = msosaHelper;
    }

    @Autowired
    public void setMss(MethodSecurityService mss) {
        this.mss = mss;
    }

    @Override
    public ElementsResponse read(String projectId, String refId, ElementsRequest req,
            Map<String, String> params) {

        String commitId = params.getOrDefault(MsosaConstants.COMMITID, null);
        NodeGetInfo info = nodePersistence.findAll(projectId, refId, commitId, req.getElements());

        if (!info.getRejected().isEmpty()) {
            //continue looking in visible mounted projects for elements if not all found
            NodeGetInfo curInfo = info;
            List<Pair<String, String>> usages = new ArrayList<>();
            getProjectUsages(projectId, refId, commitId, usages, true);

            int i = 1; //0 is entry project, already gotten
            while (!curInfo.getRejected().isEmpty() && i < usages.size()) {
                ElementsRequest reqNext = buildRequest(curInfo.getRejected().keySet());
                //TODO use the right commitId in child if commitId is present in params
                curInfo = getNodePersistence().findAll(usages.get(i).getFirst(), usages.get(i).getSecond(), "", reqNext.getElements());
                info.getActiveElementMap().putAll(curInfo.getActiveElementMap());
                curInfo.getActiveElementMap().forEach((id, json) -> info.getRejected().remove(id));
                curInfo.getRejected().forEach((id, rejection) -> {
                    if (info.getRejected().containsKey(id) && rejection.getCode() == 410) {
                        info.getRejected().put(id, rejection); //deleted element is better than not found
                    }
                });
                i++;
            }
        }

        ElementsResponse response = new ElementsResponse();
        response.getElements().addAll(info.getActiveElementMap().values());
        response.setRejected(new ArrayList<>(info.getRejected().values()));
        return response;
    }

    @Override
    public void extraProcessPostedElement(NodeChangeInfo info, ElementJson element) {
        //remove _childViews if posted
        element.remove(MsosaConstants.CHILDVIEWS);
    }

    @Override
    public MountJson getProjectUsages(String projectId, String refId, String commitId, List<Pair<String, String>> saw,
            boolean restrictOnPermissions) {
        ContextHolder.setContext(projectId, refId);
        saw.add(Pair.of(projectId, refId));
        List<ElementJson> mountsJson = nodePersistence.findAllByNodeType(projectId,refId,commitId,MsosaNodeType.PROJECTUSAGE.getValue());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<MountJson> mountValues = new ArrayList<>();

        for (ElementJson mount: mountsJson) {
            String mountedProjectId = (String)mount.get(MsosaConstants.MOUNTEDELEMENTPROJECTID);
            String mountedRefId = (String)mount.get(MsosaConstants.MOUNTEDREFID);
            if(mountedProjectId == null){
                logger.error("Could not find Mounted Project Id");
                continue;
            }
            if(mountedRefId == null){
                logger.error("Could not find Mounted Ref Id for Project ID: {}" ,mountedProjectId);
                continue;
            }
            if (saw.contains(Pair.of(mountedProjectId, mountedRefId))) {
                //prevent circular dependencies or dups - should it be by project or by project and ref?
                continue;
            }
            try {
                if (restrictOnPermissions && !mss.hasBranchPrivilege(auth, mountedProjectId, mountedRefId,
                    Privileges.BRANCH_READ.name(), true)) {
                    //should permission be considered here?
                    continue;
                }
            } catch (Exception e) {
                continue;
            }
            //doing a depth first traversal TODO get appropriate commitId
            try {
                mountValues.add(getProjectUsages(mountedProjectId, mountedRefId, "", saw, restrictOnPermissions));
            } catch (Exception e) {
                //log the error and move on
                logger.debug(String.format("Could not get project usages from nested project %s" , mountedProjectId), e);
            }
        }
        MountJson res = new MountJson();
        res.setId(projectId);
        res.setProjectId(projectId);
        res.setRefId(refId);
        res.put(MsosaConstants.MOUNTS, mountValues);
        return res;
    }
}
