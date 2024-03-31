package org.openmbee.mms.federatedpersistence.domain;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.crud.domain.NodeUpdateFilter;
import org.openmbee.mms.data.dao.CommitDAO;
import org.openmbee.mms.data.dao.NodeDAO;
import org.openmbee.mms.data.dao.NodeIndexDAO;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.crud.domain.CommitDomain;
import org.openmbee.mms.crud.domain.NodeChangeDomain;
import org.openmbee.mms.data.domains.scoped.Commit;
import org.openmbee.mms.data.domains.scoped.Node;
import org.openmbee.mms.federatedpersistence.dao.FederatedNodeChangeInfo;
import org.openmbee.mms.federatedpersistence.dao.FederatedNodeChangeInfoImpl;
import org.openmbee.mms.federatedpersistence.dao.FederatedNodeGetInfo;
import org.openmbee.mms.json.BaseJson;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;
import org.openmbee.mms.json.ElementVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class FederatedNodeChangeDomain extends NodeChangeDomain {
    private static final Logger logger = LoggerFactory.getLogger(FederatedNodeChangeDomain.class);

    protected FederatedNodeGetDomain getDomain;
    protected FederatedElementDomain elementDomain;
    protected NodeDAO nodeRepository;
    protected NodeIndexDAO nodeIndex;
    protected CommitDAO commitDAO;

    @Autowired
    public FederatedNodeChangeDomain(CommitDomain commitDomain,
            FederatedNodeGetDomain getDomain, NodeDAO nodeRepository, NodeIndexDAO nodeIndex, FederatedElementDomain elementDomain,
            List<NodeUpdateFilter> nodeUpdateFilters, CommitDAO commitDAO) {
        super(getDomain, commitDomain, nodeUpdateFilters);
        this.getDomain = getDomain;
        this.nodeRepository = nodeRepository;
        this.nodeIndex = nodeIndex;
        this.elementDomain = elementDomain;
        this.commitDAO = commitDAO;
    }

    @Override
    protected NodeChangeInfo createNodeChangeInfo() {
        return new FederatedNodeChangeInfoImpl();
    }

    @Override
    public NodeChangeInfo initInfo(CommitJson commitJson, boolean overwrite, boolean preserveTimestamps) {
        commitJson.setId(UUID.randomUUID().toString());
        commitJson.setDocId(commitJson.getId());

        NodeChangeInfo info =  super.initInfo(commitJson, overwrite, preserveTimestamps);
        if(info instanceof FederatedNodeChangeInfo) {
            ((FederatedNodeChangeInfo)info).setOldDocIds(new HashSet<>());
            ((FederatedNodeChangeInfo)info).setReqIndexIds(new HashSet<>());
            ((FederatedNodeChangeInfo)info).setToSaveNodeMap(new HashMap<>());
        } else {
            throw new InternalErrorException("Unexpected NodeChangeInfo type in FederatedNodeChangeDomain");
        }
        return info;
    }


    @Override
    public void processElementAdded(NodeChangeInfo info, ElementJson element) {
        if(!(info instanceof FederatedNodeChangeInfo)) {
            throw new InternalErrorException("Unexpected NodeChangeInfo type in FederatedNodeChangeDomain");
        }

        Node node = new Node();
        node.setNodeId(element.getId());

        ((FederatedNodeChangeInfo) info).getExistingNodeMap().put(element.getId(), node);
        super.processElementAdded(info, element);

        node.setInitialCommit(element.getCommitId());

        CommitJson commitJson = info.getCommitJson();
        ElementVersion newObj = new ElementVersion()
            .setDocId(element.getDocId())
            .setId(element.getId())
            .setType("Element");
        commitJson.getAdded().add(newObj);

        ((FederatedNodeChangeInfo) info).getToSaveNodeMap().put(element.getId(), node);
    }

    @Override
    public boolean processElementUpdated(NodeChangeInfo info, ElementJson element, ElementJson existing) {
        if(!(info instanceof FederatedNodeChangeInfo)) {
            throw new InternalErrorException("Unexpected NodeChangeInfo type in FederatedNodeChangeDomain");
        }
        String previousDocId;
        Node n = ((FederatedNodeChangeInfo) info).getExistingNodeMap().get(element.getId());
        if(n != null) {
            previousDocId = n.getDocId();
            ((FederatedNodeChangeInfo) info).getOldDocIds().add(previousDocId);
            if(n.isDeleted()) {
                existing.setIsDeleted(Constants.TRUE);
            }
        } else {
            return false;
        }

        if (!super.processElementUpdated(info, element, existing)) {
            return false;
        }
        ElementVersion newObj= new ElementVersion()
            .setPreviousDocId(previousDocId)
            .setDocId(element.getDocId())
            .setId(element.getId())
            .setType("Element");
        info.getCommitJson().getUpdated().add(newObj);
        return true;
    }

    @Override
    protected void processElementAddedOrUpdated(NodeChangeInfo info, ElementJson element) {

        if(! (info instanceof FederatedNodeChangeInfo)) {
            throw new InternalErrorException("Unexpected NodeChangeInfo type in FederatedNodeChangeDomain");
        }

        Node n = ((FederatedNodeChangeInfo) info).getExistingNodeMap().get(element.getId());
        if(n == null) {
            return;
        }

        String docId = UUID.randomUUID().toString();
        element.setDocId(docId);

        super.processElementAddedOrUpdated(info, element);
        n.setDocId(element.getDocId());
        n.setLastCommit(info.getCommitJson().getId());
        n.setDeleted(false);
        n.setNodeType(elementDomain.getNodeType(element.getProjectId(), element));

        ((FederatedNodeChangeInfo) info).getToSaveNodeMap().put(element.getId(), n);
    }

    @Override
    public void processElementDeleted(NodeChangeInfo info, ElementJson element) {
        if(! (info instanceof FederatedNodeChangeInfo)) {
            throw new InternalErrorException("Unexpected NodeChangeInfo type in FederatedNodeChangeDomain");
        }
        Node n = ((FederatedNodeChangeInfo) info).getExistingNodeMap().get(element.getId());
        if (n == null) {
            return;
        }

        super.processElementDeleted(info, element);

        ElementVersion newObj = new ElementVersion()
            .setPreviousDocId(n.getDocId())
            .setId(element.getId())
            .setType("Element");
        info.getCommitJson().getDeleted().add(newObj);
        ((FederatedNodeChangeInfo) info).getOldDocIds().add(n.getDocId());
        ((FederatedNodeChangeInfo) info).getToSaveNodeMap().put(n.getNodeId(), n);
        n.setDeleted(true);
    }

    @Override
    public FederatedNodeChangeInfo processDeleteJson(NodeChangeInfo info, Collection<ElementJson> elements) {
        if(! (info instanceof FederatedNodeChangeInfo)) {
            throw new InternalErrorException("Unexpected NodeChangeInfo type in FederatedNodeChangeDomain");
        }

        FederatedNodeChangeInfo federatedInfo = (FederatedNodeChangeInfo) info;

        for (String nodeId : info.getReqElementMap().keySet()) {
            if (!existingNodeContainsNodeId(federatedInfo, nodeId)) {
                continue;
            }
            Node node = federatedInfo.getExistingNodeMap().get(nodeId);
            ElementJson indexElement = info.getExistingElementMap().get(nodeId);

            if (node.isDeleted()) {
                info.addRejection(nodeId, new Rejection(indexElement, 410, "Already deleted"));
                continue;
            }
            if (indexElement == null) {
                logger.warn("node db and index mismatch on element delete: nodeId: " + nodeId +
                    ", docId not found: " + federatedInfo.getExistingNodeMap().get(nodeId).getDocId());
                indexElement = new ElementJson().setId(nodeId).setDocId(node.getDocId());
            }

            ElementJson request = info.getReqElementMap().get(nodeId);
            request.putAll(indexElement);
            processElementDeleted(info, request);
            info.getDeletedMap().put(nodeId, request);
        }
        return federatedInfo;
    }

    protected boolean existingNodeContainsNodeId(FederatedNodeGetInfo info, String nodeId) {
        if (!info.getExistingNodeMap().containsKey(nodeId)) {
            rejectNotFound(info, nodeId);
            return false;
        }
        return true;
    }

    // create new elastic id for all element json, update modified time, modifier (use dummy for now), set _projectId, _refId, _inRefIds
    @Override
    public FederatedNodeChangeInfo processPostJson(NodeChangeInfo info, Collection<ElementJson> elements) {

        if(!(info instanceof FederatedNodeChangeInfo)) {
            throw new InternalErrorException("Unexpected NodeChangeInfo type in FederatedNodeChangeDomain");
        }
        FederatedNodeChangeInfo federatedInfo = (FederatedNodeChangeInfo) info;

        // Logic for update/add
        for (ElementJson element : elements) {
            if (element == null) {
                continue;
            }
            boolean added = false;
            if (element.getId() == null || element.getId().isEmpty()) {
                element.setId(UUID.randomUUID().toString());
            }
            ElementJson indexElement = info.getExistingElementMap().get(element.getId());
            Node n = federatedInfo.getExistingNodeMap().get(element.getId());
            if (n == null) {
                added = true;
            } else if (indexElement == null) {
                logger.warn("node db and index mismatch on element update: nodeId: " + n.getNodeId() + ", docId not found: " + n.getDocId());
                indexElement = new ElementJson().setId(n.getNodeId()).setDocId(n.getDocId());
                Optional<Commit> init = commitDAO.findByCommitId(n.getInitialCommit());
                if (init.isPresent()) {
                    indexElement.setCreator(init.get().getCreator());
                    indexElement.setCreated(Formats.FORMATTER.format(init.get().getTimestamp()));
                }
            }

            // create new doc id for all element json, update modified time, modifier (use dummy for now), set _projectId, _refId, _inRefIds
            if (added) {
                processElementAdded(info, element);
            } else {
                processElementUpdated(info, element, indexElement);
            }
        }
        return federatedInfo;
    }


    @Override
	public void primeNodeChangeInfo(NodeChangeInfo nodeChangeInfo, Collection<ElementJson> transactedElements) {
        Set<String> elementIds = transactedElements.stream().map(BaseJson::getId).filter(id->null!=id).collect(Collectors.toSet());
        List<Node> existingNodes = nodeRepository.findAllByNodeIds(elementIds);

        if (!(nodeChangeInfo instanceof FederatedNodeChangeInfo)) {
            throw new InternalErrorException("Node processing is not using FederatedNodeChangeInfo");
        }

        FederatedNodeChangeInfo info = (FederatedNodeChangeInfo)nodeChangeInfo;
        Set<String> indexIds = Optional.ofNullable(info.getReqIndexIds()).orElse(new HashSet<>());
        Map<String, Node> existingNodeMap = Optional.ofNullable(info.getExistingNodeMap()).orElse(new HashMap<>());
        Map<String, ElementJson> reqElementMap = new HashMap<>(); // don't add to any existing since this change processing is only for transactedElements
        Map<String, ElementJson> existingElementMap = Optional.ofNullable(info.getExistingElementMap()).orElse(new HashMap<>());

        Set<String> transactedIndexIds = new HashSet<>();
        for (Node node : existingNodes) {
            transactedIndexIds.add(node.getDocId());
            indexIds.add(node.getDocId());
            existingNodeMap.put(node.getNodeId(), node);
        }
        if (!transactedElements.isEmpty()) {
            reqElementMap.putAll(convertJsonToMap(transactedElements));
        }

        // bulk read existing elements in elastic
        List<ElementJson> existingElements = nodeIndex.findAllById(transactedIndexIds);
        existingElementMap.putAll(convertJsonToMap(existingElements));

        info.setExistingElementMap(existingElementMap);
        info.setReqElementMap(reqElementMap);
        info.setRejected(Optional.ofNullable(info.getRejected()).orElse(new HashMap<>()));
        info.setActiveElementMap(Optional.ofNullable(info.getActiveElementMap()).orElse(new HashMap<>()));
        info.setExistingNodeMap(existingNodeMap);
        info.setReqIndexIds(indexIds);
	}

}
