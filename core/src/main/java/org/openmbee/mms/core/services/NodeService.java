package org.openmbee.mms.core.services;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.openmbee.mms.core.objects.ElementsCommitResponse;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;

public interface NodeService {

    void readAsStream(String projectId, String refId, Map<String, String> params, OutputStream output, String accept) throws IOException;

    ElementsResponse read(String projectId, String refId, String id, Map<String, String> params);

    ElementsResponse read(String projectId, String refId, ElementsRequest req, Map<String, String> params);

    ElementsCommitResponse createOrUpdate(String projectId, String refId, ElementsRequest req,
                                          Map<String, String> params, String user);

    ElementsCommitResponse delete(String projectId, String refId, String id, String user);

    ElementsCommitResponse delete(String projectId, String refId, ElementsRequest req, String user);
}
