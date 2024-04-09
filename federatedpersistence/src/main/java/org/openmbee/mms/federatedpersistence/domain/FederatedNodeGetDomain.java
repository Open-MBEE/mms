package org.openmbee.mms.federatedpersistence.domain;

import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.data.dao.*;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.crud.domain.NodeGetDomain;
import org.openmbee.mms.data.domains.scoped.Branch;
import org.openmbee.mms.data.domains.scoped.Commit;
import org.openmbee.mms.data.domains.scoped.Node;
import org.openmbee.mms.federatedpersistence.dao.FederatedNodeGetInfo;
import org.openmbee.mms.federatedpersistence.dao.FederatedNodeGetInfoImpl;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.openmbee.mms.core.config.ContextHolder.getContext;

@Component
public class FederatedNodeGetDomain extends NodeGetDomain {

    protected static final Logger logger = LoggerFactory.getLogger(FederatedNodeGetDomain.class);

    private NodeDAO nodeDAO;
    private NodeIndexDAO nodeIndex;
    private CommitDAO commitDAO;
    private BranchDAO branchDAO;
    private CommitIndexDAO commitIndex;

    @Autowired
    public FederatedNodeGetDomain(NodeDAO nodeDAO, NodeIndexDAO nodeIndex,
                                  CommitDAO commitDAO, BranchDAO branchDAO, CommitIndexDAO commitIndex) {
        this.nodeDAO = nodeDAO;
        this.nodeIndex = nodeIndex;
        this.commitDAO = commitDAO;
        this.branchDAO = branchDAO;
        this.commitIndex = commitIndex;
    }

    public NodeGetInfo initInfo(List<ElementJson> elements, CommitJson commitJson) {
        Set<String> indexIds = new HashSet<>();
        elements.stream().map(ElementJson::getId).forEach(indexIds::add);
        List<Node> existingNodes = nodeDAO.findAllByNodeIds(indexIds);
        NodeGetInfo nodeGetInfo = initInfoFromNodes(existingNodes, commitJson);
        nodeGetInfo.getReqElementMap().putAll(convertJsonToMap(elements));
        return nodeGetInfo;

    }

    public NodeGetInfo initInfoFromNodes(List<Node> existingNodes, CommitJson commitJson) {
        NodeGetInfo nodeGetInfo =  super.initInfo(commitJson, this::createNodeGetInfo);
        Set<String> indexIds = existingNodes.stream().map(Node::getDocId).collect(Collectors.toSet());
        if (nodeGetInfo instanceof FederatedNodeGetInfo) {
            FederatedNodeGetInfo federatedInfo = (FederatedNodeGetInfo)nodeGetInfo;
            federatedInfo.setExistingNodeMap(new HashMap<>());
            federatedInfo.setReqIndexIds(new HashSet<>());
            for (Node node : existingNodes) {
                federatedInfo.getReqIndexIds().add(node.getDocId());
                federatedInfo.getExistingNodeMap().put(node.getNodeId(), node);
                federatedInfo.getReqElementMap().put(node.getNodeId(), new ElementJson().setId(node.getNodeId()));
            }
        }
        // bulk read existing elements in elastic
        List<ElementJson> existingElements = nodeIndex.findAllById(indexIds);
        addExistingElements(nodeGetInfo, existingElements); // handeled in addExistingElements
        return nodeGetInfo;
    }

    public NodeGetInfo processGetJsonFromNodes(List<Node> nodes, String commitId) {
        if (commitId == null || commitId.isEmpty()) {
            return processGetJsonFromNodes(nodes);
        }
        NodeGetInfo info = initInfoFromNodes(nodes, null);
        return processCommit(info, commitId);
    }

    public NodeGetInfo processGetJson(List<ElementJson> elements, String commitId) {
        if (commitId == null || commitId.isEmpty()) {
            return processGetJson(elements);
        }
        NodeGetInfo info = initInfo(elements, null); //gets all current nodes
        return processCommit(info, commitId);
    }


    public NodeGetInfo processGetJsonFromNodes(List<Node> nodes) {
        NodeGetInfo info = initInfoFromNodes(nodes, null);
        return processLatest(info);
    }

    public NodeGetInfo processGetJson(List<ElementJson> elements) {
        NodeGetInfo info = initInfo(elements,null);
        return processLatest(info);
    }

    protected NodeGetInfo processLatest(NodeGetInfo info) {
        if(!(info instanceof FederatedNodeGetInfo)) {
            throw new InternalErrorException("Invalid use of FederatedNodeGetDomain");
        }
        FederatedNodeGetInfo federatedInfo = (FederatedNodeGetInfo) info;
        for (String nodeId : info.getReqElementMap().keySet()) {
            if (!existingNodeContainsNodeId(federatedInfo, nodeId)) {
                continue;
            }
            ElementJson indexElement = info.getExistingElementMap().get(nodeId);
            if (indexElement == null) {
                logger.warn("node db and index mismatch on element get: nodeId: " + nodeId +
                    ", docId not found: " + federatedInfo.getExistingNodeMap().get(nodeId).getDocId());
                rejectNotFound(info, nodeId);
                continue;
            }
            if (federatedInfo.getExistingNodeMap().get(nodeId).isDeleted()) {
                rejectDeleted(info, nodeId, indexElement);
                continue;
            }
            info.getActiveElementMap().put(nodeId, indexElement);
        }
        return info;
    }

    protected NodeGetInfo processCommit(NodeGetInfo info, String commitId) {
        if (!(info instanceof FederatedNodeGetInfo)) {
            throw new InternalErrorException("Invalid use of FederatedNodeGetDomain");
        }
        FederatedNodeGetInfo federatedInfo = (FederatedNodeGetInfo) info;
        Optional<Commit> commit = commitDAO.findByCommitId(commitId);
        if (!commit.isPresent() ) {
            throw new BadRequestException("commitId is invalid");
        }
        Instant time = commit.get().getTimestamp(); //time of commit
        List<String> refCommitIds = null; //get it later if needed
        for (String nodeId : info.getReqElementMap().keySet()) {
            if (!existingNodeContainsNodeId(federatedInfo, nodeId)) { // nodeId not found
                continue;
            }
            ElementJson indexElement = info.getExistingElementMap().get(nodeId);
            if (indexElement == null) {
                // latest element not found, mock an object to continue
                Node n = federatedInfo.getExistingNodeMap().get(nodeId);
                logger.warn("node db and index mismatch on element commit get: nodeId: " + nodeId +
                    ", docId not found: " + n.getDocId());
                Optional<Commit> last = commitDAO.findByCommitId(n.getLastCommit());
                Optional<Commit> first = commitDAO.findByCommitId(n.getInitialCommit());
                if (!last.isPresent() || !first.isPresent()) {
                    rejectNotFound(info, nodeId);
                    continue;
                }
                indexElement = new ElementJson().setId(nodeId).setDocId(n.getDocId());
                indexElement.setModified(Formats.FORMATTER.format(last.get().getTimestamp()));
                indexElement.setModifier(last.get().getCreator());
                indexElement.setCommitId(last.get().getCommitId());
                indexElement.setCreator(first.get().getCreator());
                indexElement.setCreated(Formats.FORMATTER.format(first.get().getTimestamp()));
            }
            if (info.getCommitJson() != null && info.getCommitJson().getRefId() != null) {
                indexElement.setRefId(info.getCommitJson().getRefId());
            } else {
                indexElement.setRefId(ContextHolder.getContext().getBranchId());
            }

            Instant modified = Instant.from(Formats.FORMATTER.parse(indexElement.getModified()));
            Instant created = Instant.from(Formats.FORMATTER.parse(indexElement.getCreated()));

            if (commitId.equals(indexElement.getCommitId())) { //exact match
                addActiveElement(info, nodeId, indexElement);
            } else if (created.isAfter(time)) { // element created after commit
                rejectNotFound(info, nodeId);
            } else if (modified.isAfter(time)) { // latest element is after commit
                Optional<ElementJson> tryExact = nodeIndex.getByCommitId(commitId, nodeId);
                if (tryExact.isPresent()) {
                    addActiveElement(info, nodeId, tryExact.get());
                    continue; // found exact match at commit
                }
                if (refCommitIds == null) { // need list of commitIds of current ref to filter on
                    refCommitIds = getRefCommitIds(time);
                }
                Optional<ElementJson> e = nodeIndex.getElementLessThanOrEqualTimestamp(nodeId,
                    Formats.FORMATTER.format(time), refCommitIds);
                if (e.isPresent()) { // found version of element at commit time
                    Instant realModified = Instant.from(Formats.FORMATTER.parse(e.get().getModified()));
                    if (elementDeleted(nodeId, commitId, time, realModified, refCommitIds)) {
                        rejectDeleted(info, nodeId, e.get());
                    } else {
                        addActiveElement(info, nodeId, e.get());
                    }
                } else {
                    rejectNotFound(info, nodeId); // element not found at commit time
                }
            } else if (federatedInfo.getExistingNodeMap().get(nodeId).isDeleted()) { // latest element is before commit, but deleted
                if (refCommitIds == null) { // need list of commitIds of current ref to filter on
                    refCommitIds = getRefCommitIds(time);
                }
                if (elementDeleted(nodeId, commitId, time, modified, refCommitIds)) {
                    rejectDeleted(info, nodeId, indexElement);
                } else {
                    addActiveElement(info, nodeId, indexElement);
                }
            } else { // latest element version is version at commit, not deleted
                addActiveElement(info, nodeId, indexElement);
            }
        }
        return info;
    }

    private boolean elementDeleted(String nodeId, String commitId, Instant time, Instant modified, List<String> refCommitIds) {
        List<CommitJson> commits = commitIndex.elementDeletedHistory(nodeId, refCommitIds);
        for (CommitJson c: commits) {
            Instant deletedTime = Instant.from(Formats.FORMATTER.parse(c.getCreated()));
            if ((deletedTime.isBefore(time) || c.getId().equals(commitId)) && deletedTime.isAfter(modified)) {
                //there's a delete between element last modified time and requested commit time
                //or element is deleted at commit
                return true;
            }
        }
        return false;
    }

    public boolean existingNodeContainsNodeId(FederatedNodeGetInfo info, String nodeId) {
        if (!info.getExistingNodeMap().containsKey(nodeId)) {
            rejectNotFound(info, nodeId);
            return false;
        }
        return true;
    }

    protected void addActiveElement(NodeGetInfo info, String nodeId, ElementJson indexElement) {
        info.getActiveElementMap().put(nodeId, indexElement);
    }

    protected List<String> getRefCommitIds(Instant time) {
        List<String> commitIds = new ArrayList<>();

        Optional<Branch> ref = branchDAO.findByBranchId(getContext().getBranchId());
        ref.ifPresent(current -> {
            List<Commit> refCommits = commitDAO.findByRefAndTimestampAndLimit(current, time, 0);
            for (Commit c : refCommits) {
                commitIds.add(c.getCommitId());
            }
        });
        return commitIds;
    }

    @Override
    public void addExistingElements(NodeGetInfo info, List<ElementJson> elements) {
        super.addExistingElements(info, elements);
    }

    @Override
    public NodeGetInfo createNodeGetInfo() {  //ToDo :: check
        return new FederatedNodeGetInfoImpl();
    }
}
