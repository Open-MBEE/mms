package org.openmbee.mms.artifacts.crud;

import org.openmbee.mms.artifacts.json.ArtifactJson;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.crud.domain.NodeUpdateFilter;
import org.openmbee.mms.json.ElementJson;
import org.springframework.stereotype.Component;

@Component
public class ArtifactsPersistenceNodeUpdateFilter implements NodeUpdateFilter {
    @Override
    public boolean filterUpdate(NodeChangeInfo nodeChangeInfo, ElementJson updated, ElementJson existing) {
        if(ArtifactsContext.isArtifactContext()) {
            return true;
        }
        //Ensure artifacts aren't cleared or added by the regular element update process
        if(existing.containsKey(ArtifactJson.ARTIFACTS)) {
            updated.put(ArtifactJson.ARTIFACTS, existing.get(ArtifactJson.ARTIFACTS));
        } else {
            updated.remove(ArtifactJson.ARTIFACTS);
        }
        return true;
    }
}
