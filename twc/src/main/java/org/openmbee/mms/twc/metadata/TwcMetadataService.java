package org.openmbee.mms.twc.metadata;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.twc.constants.TwcConstants;
import org.openmbee.mms.json.ProjectJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TwcMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(TwcMetadataService.class);
    private ProjectPersistence projectPersistence;

    @Autowired
    public void setProjectIndex(ProjectPersistence projectPersistence) {
        this.projectPersistence = projectPersistence;
    }

    public void updateTwcMetadata(ProjectJson projectJson, TwcMetadata metadata) {
        Map<String, String> metadataMap = metadata.toMap();
        metadataMap.put(TwcConstants.ENABLED_KEY, Constants.TRUE);
        projectJson.put(TwcConstants.FOREIGN_PROJECT, metadataMap);
        projectPersistence.update(projectJson);
    }

    public TwcMetadata getTwcMetadata(ProjectJson projectJson) {
        Map<String, Object> metadata = (Map)projectJson.get(TwcConstants.FOREIGN_PROJECT);
        if(metadata == null) {
            return null;
        }

        TwcMetadata twcMetadata = new TwcMetadata();
        twcMetadata.setHost(String.valueOf(metadata.get(TwcConstants.HOST_KEY)));
        twcMetadata.setWorkspaceId(String.valueOf(metadata.get(TwcConstants.WORKSPACE_ID_KEY)));
        twcMetadata.setResourceId(String.valueOf(metadata.get(TwcConstants.RESOURCE_ID_KEY)));
        return twcMetadata;
    }

    public void deleteTwcMetadata(ProjectJson projectJson) {
        Map<String, Object> metadata = (Map)projectJson.get(TwcConstants.FOREIGN_PROJECT);
        if(metadata == null) {
            return;
        }
        metadata.put(TwcConstants.ENABLED_KEY, Constants.FALSE);
        projectJson.put(TwcConstants.FOREIGN_PROJECT, metadata);
        projectPersistence.update(projectJson);
    }

}
