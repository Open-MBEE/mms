package org.openmbee.mms.storage;



import java.io.IOException;
import java.io.OutputStream;

import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.json.ElementJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

/**
 * Azure Blob Storage for Artifacts
 */
@Component
@Conditional(AzureBlobStorageCondition.class)
public class AzureBlobStorage extends AbstractArtifactStorage {
    static final String BLOB_RESOURCE_PATTERN = "azure-blob://%s/%s";

    @Value("${spring.cloud.azure.storage.blob.container-name}")
    private String containerName;

    @Autowired
    private ResourceLoader resourceLoader;

    @Override
    public byte[] get(String location, ElementJson element, String mimetype) {
        try {
            return resourceLoader.getResource(location).getInputStream().readAllBytes();
        } catch (IOException ioe) {
            throw new NotFoundException(ioe);
        }
    }

    @Override
    public String store(byte[] data, ElementJson element, String mimetype) {
        String location = String.format(BLOB_RESOURCE_PATTERN, this.containerName, buildLocation(element, mimetype));
        Resource resource = resourceLoader.getResource(location);
        try (OutputStream os = ((WritableResource) resource).getOutputStream()) {
            os.write(data);
        } catch (IOException e) {
            logger.error("Error storing artifact: ", e);
            throw new InternalErrorException(e);
        }
        return location;
    }
}