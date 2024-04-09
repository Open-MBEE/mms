package org.openmbee.mms.cameo.services;

import org.openmbee.mms.cameo.CameoConstants;
import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.core.services.ProjectService;
import org.openmbee.mms.crud.services.DefaultProjectService;
import org.openmbee.mms.json.ElementJson;
import org.openmbee.mms.json.ProjectJson;
import org.openmbee.mms.json.RefJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;


@Service("cameoProjectService")
public class CameoProjectService extends DefaultProjectService implements ProjectService {

    private NodeService nodeService;

    @Autowired
    @Qualifier("cameoNodeService")
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Override
    public ProjectJson create(ProjectJson project) {
        ProjectJson projectJson = super.create(project);

        ElementJson projectHoldingBin = createNode(CameoConstants.HOLDING_BIN_PREFIX + projectJson.getProjectId(),
            "Holding Bin", projectJson);

        ElementJson viewInstanceBin = createNode(CameoConstants.VIEW_INSTANCES_BIN_PREFIX + projectJson.getProjectId(),
            "View Instances Bin", projectJson);

        ElementsRequest elementsRequest = new ElementsRequest();
        elementsRequest.setElements(List.of(projectHoldingBin, viewInstanceBin));
        nodeService.createOrUpdate(projectJson.getProjectId(), Constants.MASTER_BRANCH, elementsRequest,
            Collections.EMPTY_MAP, projectJson.getCreator());

        return projectJson;
    }

    @Override
    public RefJson createMasterRefJson(ProjectJson project){
        RefJson branchJson = super.createMasterRefJson(project);
        branchJson.put("twcId",Constants.MASTER_BRANCH);
        return branchJson;
    }

    private static ElementJson createNode(String id, String name, ProjectJson projectJson) {
        ElementJson e = new ElementJson();
        e.setId(id);
        e.setName(name);
        e.put(CameoConstants.OWNERID, projectJson.getProjectId());
        e.put(CameoConstants.TYPE, CameoConstants.PACKAGE_TYPE);
        e.put(CameoConstants.ISGROUP, false);
        e.put(CameoConstants.DOCUMENTATION, "");
        e.put(CameoConstants.VISIBILITY, CameoConstants.PUBLIC_VISIBILITY);
        return e;
    }
}
