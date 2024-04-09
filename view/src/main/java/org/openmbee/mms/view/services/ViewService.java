package org.openmbee.mms.view.services;

import java.util.Map;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.core.services.HierarchicalNodeService;

public interface ViewService extends HierarchicalNodeService {

    ElementsResponse getDocuments(String projectId, String refId, Map<String, String> params);

    ElementsResponse getView(String projectId, String refId, String elementId, Map<String, String> params);

    ElementsResponse getViews(String projectId, String refId, ElementsRequest req, Map<String, String> params);

    void addChildViews(ElementsResponse res, Map<String, String> params);

    ElementsResponse getGroups(String projectId, String refId, Map<String, String> params);
}
