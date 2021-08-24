package org.openmbee.mms.crud.services;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import java.util.Optional;
import java.util.UUID;
import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.data.domains.scoped.Commit;
import org.openmbee.mms.json.BaseJson;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;
import org.openmbee.mms.data.domains.scoped.Node;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class NodePostHelper extends NodeOperation {

    public boolean isUpdated(BaseJson element, Map<String, Object> existing,
                             NodeChangeInfo info) {

        if (element.isPartialOf(existing)) {
            info.addRejection(element.getId(), new Rejection(element, 304, "Is Equivalent"));
            return false;
        }
        return true;
    }

    public boolean diffUpdateJson(BaseJson element, Map<String, Object> existing,
                                  NodeChangeInfo info) {

        String jsonModified = element.getModified();
        Object existingModified = existing.get(BaseJson.MODIFIED);
        if (jsonModified != null && !jsonModified.isEmpty() && existingModified != null) {
            try {
                Date jsonModDate = Formats.SDF.parse(jsonModified);
                Date existingModDate = Formats.SDF.parse(existingModified.toString());
                if (jsonModDate.before(existingModDate)) {
                    info.addRejection(element.getId(), new Rejection(element, 409, "Conflict Detected"));
                    return false;
                }
            } catch (ParseException e) {
                logger.info("date parse exception:" + jsonModified + " " + existingModified);
            }
        }
        element.merge(existing);
        return true;
    }

    // create new elastic id for all element json, update modified time, modifier (use dummy for now), set _projectId, _refId, _inRefIds
    public NodeChangeInfo processPostJson(List<ElementJson> elements, boolean overwriteJson,
                                          CommitJson cmjs, NodeService service) {

        NodeChangeInfo info = initInfo(elements, cmjs);

        // Logic for update/add
        for (ElementJson element : elements) {
            if (element == null) {
                continue;
            }
            boolean added = false;
            boolean updated = false;
            if (element.getId() == null || element.getId().isEmpty()) {
                element.setId(UUID.randomUUID().toString());
            }
            ElementJson indexElement = info.getExistingElementMap().get(element.getId());
            Node n = info.getExistingNodeMap().get(element.getId());
            if (n == null) {
                added = true;
            } else if (indexElement == null) {
                logger.warn("node db and index mismatch on element update: nodeId: " + n.getNodeId() + ", docId not found: " + n.getDocId());
                //info.addRejection(element.getId(), new Rejection(element, 500, "Update failed: previous element not found"));
                //continue;
                indexElement = new ElementJson().setId(n.getNodeId()).setDocId(n.getDocId());
                Optional<Commit> init = commitRepository.findByCommitId(n.getInitialCommit());
                if (init.isPresent()) {
                    indexElement.setCreator(init.get().getCreator());
                    indexElement.setCreated(formatter.format(init.get().getTimestamp()));
                }
            }

            if(! validateOperation(added ? Operation.ADD : Operation.UPDATE, element, indexElement, overwriteJson)) {
                info.addRejection(element.getId(), new Rejection(element, 406, "Invalid operation"));
                continue;
            }

            if (!added) {
                if (!overwriteJson) {
                    if (n.isDeleted() || isUpdated(element, indexElement, info)) {
                        updated = diffUpdateJson(element, indexElement, info);
                    }
                } else {
                    updated = true;
                    element.setCreated(indexElement.getCreated());
                    element.setCreator(indexElement.getCreator());
                }
            }

            // create new elastic id for all element json, update modified time, modifier (use dummy for now), set _projectId, _refId, _inRefIds
            if (added) {
                Node node = new Node();
                processElementAdded(element, node, info);
                service.extraProcessPostedElement(element, node, info);
            } else if (updated) {
                Node node = info.getExistingNodeMap().get(element.getId());
                processElementUpdated(element, node, info);
                service.extraProcessPostedElement(element, node, info);
            }
        }
        return info;
    }
}
