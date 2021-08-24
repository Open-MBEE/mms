package org.openmbee.mms.crud.services;

import java.util.List;
import java.util.Map;

import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.data.domains.scoped.Node;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class NodeDeleteHelper extends NodeOperation {

    public NodeChangeInfo processDeleteJson(List<ElementJson> elements, CommitJson cmjs, NodeService service) {
        NodeChangeInfo info = initInfo(elements, cmjs);

        for (String nodeId : info.getReqElementMap().keySet()) {
            if (!existingNodeContainsNodeId(info, nodeId)) {
                continue;
            }
            Node node = info.getExistingNodeMap().get(nodeId);
            Map<String, Object> indexElement = info.getExistingElementMap().get(nodeId);
            if(! validateOperation(Operation.REMOVE, null, indexElement, false)) {
                info.addRejection(nodeId, new Rejection(nodeId, 406, "Invalid operation"));
                continue;
            }
            if (node.isDeleted()) {
                info.addRejection(nodeId, new Rejection(indexElement, 304, "Already deleted"));
                continue;
            }
            if (indexElement == null) {
                logger.warn("node db and index mismatch on element delete: nodeId: " + nodeId +
                    ", docId not found: " + info.getExistingNodeMap().get(nodeId).getDocId());
                indexElement = Map.of("id", nodeId);
            }
            ElementJson request = info.getReqElementMap().get(nodeId);
            request.putAll(indexElement);
            processElementDeleted(request, node, info);
            service.extraProcessDeletedElement(request, node, info);
            info.getDeletedMap().put(nodeId, request);
        }
        return info;
    }
}
