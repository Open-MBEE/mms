package org.openmbee.mms.crud.services;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.core.dao.BranchDAO;
import org.openmbee.mms.core.dao.CommitDAO;
import org.openmbee.mms.data.domains.scoped.Node;
import org.openmbee.mms.core.dao.NodeDAO;
import org.openmbee.mms.core.dao.NodeIndexDAO;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementVersion;
import org.openmbee.mms.json.ElementJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NodeOperation {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected NodeDAO nodeRepository;
    protected NodeIndexDAO nodeIndex;
    protected CommitDAO commitRepository;
    protected BranchDAO branchRepository;

    protected DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(
            ZoneId.systemDefault());

    private boolean preserveTimestamps = false;

    protected enum Operation {GET, ADD, UPDATE, REMOVE}

    @Autowired
    public void setNodeRepository(NodeDAO nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Autowired
    public void setNodeIndex(NodeIndexDAO nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    @Autowired
    public void setCommitRepository(CommitDAO commitRepository) {
        this.commitRepository = commitRepository;
    }

    @Autowired
    public void setBranchRepository(BranchDAO branchRepository) {
        this.branchRepository = branchRepository;
    }

    public void initCommitJson(CommitJson cmjs, Instant now) {
        if (cmjs.getId() == null || cmjs.getId().isEmpty()) {
            cmjs.setId(UUID.randomUUID().toString());
            cmjs.setDocId(cmjs.getId());
        }
        if (cmjs.getDocId() == null || cmjs.getDocId().isEmpty()) {
            cmjs.setDocId(UUID.randomUUID().toString());
        }
        cmjs.setCreated(formatter.format(now));
        cmjs.setAdded(new ArrayList<>());
        cmjs.setDeleted(new ArrayList<>());
        cmjs.setUpdated(new ArrayList<>());
        cmjs.setType("Commit");
    }

    public NodeChangeInfo initInfoFromNodes(List<Node> existingNodes, CommitJson cmjs) {
        Set<String> indexIds = new HashSet<>();
        Map<String, Node> existingNodeMap = new HashMap<>();
        Map<String, ElementJson> reqElementMap = new HashMap<>();
        for (Node node : existingNodes) {
            indexIds.add(node.getDocId());
            existingNodeMap.put(node.getNodeId(), node);
            reqElementMap.put(node.getNodeId(), new ElementJson().setId(node.getNodeId()));
        }
        // bulk read existing elements in elastic
        List<ElementJson> existingElements = nodeIndex.findAllById(indexIds);
        Map<String, ElementJson> existingElementMap = convertJsonToMap(existingElements);

        Instant now = Instant.now();
        if (cmjs != null) {
            initCommitJson(cmjs, now);
        }

        NodeChangeInfo info = new NodeChangeInfo();
        info.setCommitJson(cmjs);
        info.setUpdatedMap(new HashMap<>());
        info.setDeletedMap(new HashMap<>());
        info.setExistingElementMap(existingElementMap);
        info.setExistingNodeMap(existingNodeMap);
        info.setReqElementMap(reqElementMap);
        info.setReqIndexIds(indexIds);
        info.setToSaveNodeMap(new HashMap<>());
        info.setRejected(new HashMap<>());
        info.setNow(now);
        info.setOldDocIds(new HashSet<>());
        info.setActiveElementMap(new HashMap<>());
        return info;
    }

    public NodeChangeInfo initInfo(List<ElementJson> elements, CommitJson cmjs) {
        Map<String, ElementJson> reqElementMap = convertJsonToMap(elements);
        List<Node> existingNodes = nodeRepository.findAllByNodeIds(reqElementMap.keySet());
        NodeChangeInfo info = initInfoFromNodes(existingNodes, cmjs);
        info.setReqElementMap(reqElementMap);
        return info;
    }

    public void processElementAdded(ElementJson e, Node n, NodeChangeInfo info) {
        CommitJson cmjs = info.getCommitJson();
        processElementAddedOrUpdated(e, n, info);

        e.setCreator(cmjs.getCreator()); //Only set on creation of new element
        e.setCreated(cmjs.getCreated());

        ElementVersion newObj = new ElementVersion()
            .setDocId(e.getDocId())
            .setId(e.getId())
            .setType("Element");
        cmjs.getAdded().add(newObj);

        n.setNodeId(e.getId());
        n.setInitialCommit(e.getCommitId());
    }

    public void processElementUpdated(ElementJson e, Node n, NodeChangeInfo info) {
        String previousDocId = n.getDocId();
        processElementAddedOrUpdated(e, n, info);

        info.getOldDocIds().add(previousDocId);
        ElementVersion newObj= new ElementVersion()
            .setPreviousDocId(previousDocId)
            .setDocId(e.getDocId())
            .setId(e.getId())
            .setType("Element");
        info.getCommitJson().getUpdated().add(newObj);
    }

    private void processElementAddedOrUpdated(ElementJson e, Node n, NodeChangeInfo info) {
        CommitJson cmjs = info.getCommitJson();
        e.setProjectId(cmjs.getProjectId());
        e.setRefId(cmjs.getRefId());
        List<String> inRefIds = new ArrayList<>();
        inRefIds.add(cmjs.getRefId());
        e.setInRefIds(inRefIds);
        String docId = UUID.randomUUID().toString();
        e.setDocId(docId);
        e.setCommitId(cmjs.getId());

        if(!preserveTimestamps) {
            e.setModified(cmjs.getCreated());
            e.setModifier(cmjs.getCreator());
        }

        n.setDocId(e.getDocId());
        n.setLastCommit(cmjs.getId());
        n.setDeleted(false);
        n.setNodeType(0);

        info.getToSaveNodeMap().put(e.getId(), n);
        info.getUpdatedMap().put(e.getId(), e);
    }

    public void processElementDeleted(ElementJson e, Node n, NodeChangeInfo info) {
        ElementVersion newObj = new ElementVersion()
            .setPreviousDocId(n.getDocId())
            .setId(e.getId())
            .setType("Element");
        info.getCommitJson().getDeleted().add(newObj);
        info.getOldDocIds().add(n.getDocId());
        info.getToSaveNodeMap().put(n.getNodeId(), n);
        n.setDeleted(true);
    }

    public boolean existingNodeContainsNodeId(NodeGetInfo info, String nodeId) {
        if (!info.getExistingNodeMap().containsKey(nodeId)) {
            rejectNotFound(info, nodeId);
            return false;
        }
        return true;
    }

    protected void rejectNotFound(NodeGetInfo info, String nodeId) {
        info.addRejection(nodeId, new Rejection(nodeId, 404, "Not Found"));
    }

    public static Map<String, ElementJson> convertJsonToMap(List<ElementJson> elements) {
        Map<String, ElementJson> result = new HashMap<>();
        for (ElementJson elem : elements) {
            if (elem == null) {
                continue;
            }
            if (elem.getId() != null && !elem.getId().equals("")) {
                result.put(elem.getId(), elem);
            }
        }
        return result;
    }

    public static Map<String, Node> convertNodesToMap(List<Node> nodes) {
        Map<String, Node> result = new HashMap<>();
        for (Node node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.getNodeId() != null && !node.getNodeId().equals("")) {
                result.put(node.getNodeId(), node);
            }
        }
        return result;
    }

    public static List<ElementJson> sort(List<String> ids, List<ElementJson> orig) {
        Map<String, ElementJson> map = convertJsonToMap(orig);
        List<ElementJson> ret = new ArrayList<>();
        for (String id: ids) {
            if (map.containsKey(id)) {
                ret.add(map.get(id));
            }
        }
        return ret;
    }

    //find first element of type in types following e's relkey (assuming relkey's value is an element id)
    public Optional<ElementJson> getFirstRelationshipOfType(ElementJson e, List<Integer> types, String relkey) {
        //only for latest graph
        String nextId = (String)e.get(relkey);
        if (nextId == null || nextId.isEmpty()) {
            return Optional.empty();
        }
        Optional<Node> nextNode = nodeRepository.findByNodeId(nextId);
        while (nextNode.isPresent() && !nextNode.get().isDeleted()) {
            Optional<ElementJson> nextJson = nodeIndex.findById(nextNode.get().getDocId());
            if (!nextJson.isPresent()) {
                return Optional.empty();
            }
            if (types.contains(nextNode.get().getNodeType())) {
                return nextJson;
            }
            nextId = (String)nextJson.get().get(relkey);
            if (nextId == null || nextId.isEmpty()) {
                return Optional.empty();
            }
            nextNode = nodeRepository.findByNodeId(nextId);
        }
        return Optional.empty();
    }

    public boolean isPreserveTimestamps() {
        return preserveTimestamps;
    }

    public void setPreserveTimestamps(boolean preserveTimestamps) {
        this.preserveTimestamps = preserveTimestamps;
    }


    protected boolean validateOperation(Operation operation, Map<String, Object> newElement, Map<String, Object> existingElement, boolean overwrite) {
        return true;
    }
}
