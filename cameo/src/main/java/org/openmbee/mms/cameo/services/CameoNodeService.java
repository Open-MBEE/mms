package org.openmbee.mms.cameo.services;

import org.openmbee.mms.cameo.CameoConstants;
import org.openmbee.mms.cameo.CameoNodeType;
import org.openmbee.mms.core.config.Privileges;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.core.security.MethodSecurityService;
import org.openmbee.mms.core.services.HierarchicalNodeService;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.crud.CrudConstants;
import org.openmbee.mms.crud.services.DefaultNodeService;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;
import org.openmbee.mms.json.MountJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("cameoNodeService")
public class CameoNodeService extends DefaultNodeService implements HierarchicalNodeService {

    protected CameoHelper cameoHelper;
    private MethodSecurityService mss;

    @Autowired
    public void setCameoHelper(CameoHelper cameoHelper) {
        this.cameoHelper = cameoHelper;
    }

    @Autowired
    public void setMss(MethodSecurityService mss) {
        this.mss = mss;
    }

    @Override
    public ElementsResponse read(String projectId, String refId, ElementsRequest req,
            Map<String, String> params) {

        String commitId = params.getOrDefault(CrudConstants.COMMITID, null);
        if (commitId == null && !optimizationConfig.isOptimizeForFederated()) {
            Optional<CommitJson> commitJson = commitPersistence.findLatestByProjectAndRef(projectId, refId);
            if (!commitJson.isPresent()) {
                throw new InternalErrorException("Could not find latest commit for project and ref");
            }
            commitId = commitJson.get().getId();
        }

        NodeGetInfo info = getNodePersistence().findAll(projectId, refId, commitId, req.getElements());
        info.getActiveElementMap().values().forEach((e) -> e.setRefId(refId));
        if (!info.getRejected().isEmpty()) {
            //continue looking in visible mounted projects for elements if not all found
            NodeGetInfo curInfo = info;
            List<Pair<String, String>> usages = new ArrayList<>();
            getProjectUsages(projectId, refId, commitId, usages, true);

            int i = 1; //0 is entry project, already gotten
            while (!curInfo.getRejected().isEmpty() && i < usages.size()) {
                final int j = i;
                ElementsRequest reqNext = buildRequest(curInfo.getRejected().keySet());
                //TODO use the right commitId in child if commitId is present in params :: same commit Id is not working for child
                curInfo = getNodePersistence().findAll(usages.get(i).getFirst(), usages.get(i).getSecond(), "", reqNext.getElements());
                curInfo.getActiveElementMap().values().forEach((e) -> e.setRefId(usages.get(j).getSecond()));
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
        response.setCommitId(commitId);
        return response;
    }

    @Override
    public void extraProcessPostedElement(NodeChangeInfo info, ElementJson element) {
        //remove _childViews if posted
        element.remove(CameoConstants.CHILDVIEWS);
    }

    @Override
    public MountJson getProjectUsages(String projectId, String refId, String commitId, List<Pair<String, String>> saw,
            boolean restrictOnPermissions) {
        saw.add(Pair.of(projectId, refId));
        List<ElementJson> mounts = getNodePersistence().findAllByNodeType(projectId, refId, commitId,
            CameoNodeType.PROJECTUSAGE.getValue());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<MountJson> mountValues = new ArrayList<>();
        for (ElementJson mount : mounts) {
            String mountedProjectId = (String)mount.get(CameoConstants.MOUNTEDELEMENTPROJECTID);
            String mountedRefId = (String)mount.get(CameoConstants.MOUNTEDREFID);
            if (mountedProjectId == null) {
                logger.error("Could not find Mounted Project Id");
                continue;
            }
            if (mountedRefId == null) {
                logger.error("Could not find Mounted Ref Id for Project ID: {}" , mountedProjectId);
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
        res.put(CameoConstants.MOUNTS, mountValues);
        return res;
    }
}
