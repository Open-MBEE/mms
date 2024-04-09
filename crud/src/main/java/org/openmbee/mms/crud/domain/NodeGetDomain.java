package org.openmbee.mms.crud.domain;

import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.core.services.NodeGetInfoImpl;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NodeGetDomain extends JsonDomain {

    public NodeGetInfo initInfo(CommitJson commitJson) {
        return initInfo(commitJson, this::createNodeGetInfo);
    }

    public NodeGetInfo initInfo(CommitJson commitJson, NodeGetInfoFactory nodeGetInfoFactory) {
        Map<String, ElementJson> existingElementMap = new HashMap<>();
        Map<String, ElementJson> elementMapForRequests = new HashMap<>();

        NodeGetInfo info = nodeGetInfoFactory.get();

        info.setCommitJson(commitJson);
        info.setExistingElementMap(existingElementMap);
        info.setReqElementMap(elementMapForRequests);
        info.setRejected(new HashMap<>());
        info.setActiveElementMap(new HashMap<>());

        return info;
    }

    protected void rejectNotFound(NodeGetInfo info, String elementId) {
        info.addRejection(elementId, new Rejection(elementId, 404, "Not Found"));
    }

    protected void rejectDeleted(NodeGetInfo info, String nodeId, ElementJson indexElement) {
        info.addRejection(nodeId, new Rejection(indexElement, 410, "Element deleted"));
    }

    public NodeGetInfo createNodeGetInfo() {
        return new NodeGetInfoImpl();
    }

    public void addExistingElements(NodeGetInfo info, List<ElementJson> elements) {
        elements.forEach(e -> {
            info.getExistingElementMap().put(e.getId(), e);
        });
    }
}
