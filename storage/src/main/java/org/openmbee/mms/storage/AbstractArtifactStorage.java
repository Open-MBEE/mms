package org.openmbee.mms.storage;

import java.util.Date;

import org.apache.tika.mime.MimeTypes;
import org.openmbee.mms.artifacts.storage.ArtifactStorage;
import org.openmbee.mms.json.ElementJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractArtifactStorage implements ArtifactStorage {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();
    
    protected String buildLocation(ElementJson element, String mimetype) {
        Date today = new Date();
        return String.format("%s/%s/%s/%d", element.getProjectId(), element.getId(), getExtension(mimetype), today.getTime());
    }

    private String getExtension(String mime) {
        String extension = "";
        try {
            extension = mimeTypes.forName(mime).getExtension().substring(1);
        } catch (Exception e) {
            logger.error("Error getting extension: ", e);
        }
        return extension;
    }

    
}
