package org.openmbee.mms.msosa.services;

import org.openmbee.mms.msosa.MsosaConstants;
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


@Service("msosaProjectService")
public class MsosaProjectService extends DefaultProjectService implements ProjectService {

    private NodeService nodeService;

    @Autowired
    @Qualifier("msosaNodeService")
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Override
    public ProjectJson create(ProjectJson project) {
        ProjectJson projectJson = super.create(project);

        ElementJson projectHoldingBin = createNode(MsosaConstants.HOLDING_BIN_PREFIX + projectJson.getProjectId(),
            "Holding Bin", projectJson);

        ElementJson viewInstanceBin = createNode(MsosaConstants.VIEW_INSTANCES_BIN_PREFIX + projectJson.getProjectId(),
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
        e.put(MsosaConstants.OWNERID, projectJson.getProjectId());
        e.put(MsosaConstants.TYPE, MsosaConstants.PACKAGE_TYPE);
        e.put(MsosaConstants.ISGROUP, false);
        e.put(MsosaConstants.DOCUMENTATION, "");
        e.put(MsosaConstants.VISIBILITY, MsosaConstants.PUBLIC_VISIBILITY);
        return e;
    }
}
