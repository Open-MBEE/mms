package org.openmbee.mms.cameo.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openmbee.mms.cameo.CameoNodeType;
import org.openmbee.mms.cameo.CameoConstants;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.config.Privileges;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.security.MethodSecurityService;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.json.ElementJson;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.crud.services.DefaultNodeService;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.data.domains.scoped.Node;
import org.openmbee.mms.json.MountJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("cameoNodeService")
public class CameoNodeService extends DefaultNodeService implements NodeService {

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

        String commitId = params.getOrDefault("commitId", null);
        ContextHolder.setContext(projectId, refId);
        NodeGetInfo info = getNodeGetHelper().processGetJson(req.getElements(), commitId, this);

        if (!info.getRejected().isEmpty()) {
            //continue looking in visible mounted projects for elements if not all found
            NodeGetInfo curInfo = info;
            List<Pair<String, String>> usages = new ArrayList<>();
            getProjectUsages(projectId, refId, commitId, usages);

            int i = 1; //0 is entry project, already gotten
            while (!curInfo.getRejected().isEmpty() && i < usages.size()) {
                ElementsRequest reqNext = buildRequest(curInfo.getRejected().keySet());
                ContextHolder.setContext(usages.get(i).getFirst(), usages.get(i).getSecond());
                //TODO use the right commitId in child if commitId is present in params
                curInfo = getNodeGetHelper().processGetJson(reqNext.getElements(), "", this);
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
    public void extraProcessPostedElement(ElementJson element, Node node, NodeChangeInfo info) {
        node.setNodeType(cameoHelper.getNodeType(element).getValue());
        //remove _childViews if posted
        element.remove(CameoConstants.CHILDVIEWS);
    }

    @Override
    public void extraProcessGotElement(ElementJson element, Node node, NodeGetInfo info) {
        //TODO extended info? (qualified name/id)
    }

    public MountJson getProjectUsages(String projectId, String refId, String commitId, List<Pair<String, String>> saw) {
        ContextHolder.setContext(projectId, refId);
        saw.add(Pair.of(projectId, refId));
        List<Node> mountNodes = nodeRepository.findAllByNodeType(CameoNodeType.PROJECTUSAGE.getValue());
        Set<String> mountIds = new HashSet<>();
        mountNodes.forEach(n -> mountIds.add(n.getNodeId()));
        Map<String, String> params = new HashMap<>();
        params.put("commitId", commitId);
        ElementsResponse mountsJson = super.read(projectId, refId, buildRequest(mountIds), params);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<MountJson> mountValues = new ArrayList<>();
        for (ElementJson mount: mountsJson.getElements()) {
            String mountedProjectId = (String)mount.get(CameoConstants.MOUNTEDELEMENTPROJECTID);
            String mountedRefId = (String)mount.get(CameoConstants.MOUNTEDREFID);
            if (saw.contains(Pair.of(mountedProjectId, mountedRefId))) {
                //prevent circular dependencies or dups - should it be by project or by project and ref?
                continue;
            }
            try {
                if (!mss.hasBranchPrivilege(auth, mountedProjectId, mountedRefId,
                    Privileges.BRANCH_READ.name(), true)) {
                    //should permission be considered here?
                    continue;
                }
            } catch (Exception e) {
                continue;
            }
            //doing a depth first traversal TODO get appropriate commitId
            mountValues.add(getProjectUsages(mountedProjectId, mountedRefId, "", saw));
        }
        MountJson res = new MountJson();
        res.setId(projectId);
        res.setProjectId(projectId);
        res.setRefId(refId);
        res.put(CameoConstants.MOUNTS, mountValues);
        return res;
    }
}
