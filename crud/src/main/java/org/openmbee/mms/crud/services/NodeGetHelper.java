package org.openmbee.mms.crud.services;

import java.time.Instant;
import java.util.*;

import java.util.stream.Collectors;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.data.domains.scoped.Branch;
import org.openmbee.mms.data.domains.scoped.Commit;
import org.openmbee.mms.data.domains.scoped.Node;
import org.openmbee.mms.json.ElementJson;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static org.openmbee.mms.core.config.ContextHolder.getContext;

@Service
@Primary
public class NodeGetHelper extends NodeOperation {

    public NodeGetInfo processGetJsonFromNodes(List<Node> nodes, NodeService service) {
        NodeGetInfo info = initInfoFromNodes(nodes, null);
        return processLatest(info, service);
    }

    public NodeGetInfo processGetJson(List<ElementJson> elements, NodeService service) {
        NodeGetInfo info = initInfo(elements, null);
        return processLatest(info, service);
    }

    private NodeGetInfo processLatest(NodeGetInfo info, NodeService service) {
        for (String nodeId : info.getReqElementMap().keySet()) {
            if (!existingNodeContainsNodeId(info, nodeId)) {
                continue;
            }
            ElementJson indexElement = info.getExistingElementMap().get(nodeId);
			
			if(!validateOperation(Operation.GET, null, indexElement, false)) {
                info.addRejection(nodeId, new Rejection(nodeId, 406, "Invalid operation"));
                continue;
            }
			
            if (info.getExistingNodeMap().get(nodeId).isDeleted()) {
                rejectDeleted(info, nodeId, indexElement == null ? new ElementJson().setId(nodeId) : indexElement);
                continue;
            }
            if (indexElement == null) {
                logger.warn("node db and index mismatch on element get: nodeId: " + nodeId +
                    ", docId not found: " + info.getExistingNodeMap().get(nodeId).getDocId());
                rejectNotFound(info, nodeId);
                continue;
            }
            if (service != null) {
                service.extraProcessGotElement(indexElement, info.getExistingNodeMap().get(nodeId), info);
            }
            info.getActiveElementMap().put(nodeId, indexElement);
        }
        return info;
    }

    public NodeGetInfo processGetJsonFromNodes(List<Node> nodes, String commitId, NodeService service) {
        if (commitId == null || commitId.isEmpty()) {
            return processGetJsonFromNodes(nodes, service);
        }
        NodeGetInfo info = initInfoFromNodes(nodes, null);
        return processCommit(info, commitId, service);
    }

    public NodeGetInfo processGetJson(List<ElementJson> elements, String commitId, NodeService service) {
        if (commitId == null || commitId.isEmpty()) {
            return processGetJson(elements, service);
        }
        NodeGetInfo info = initInfo(elements, null); //gets all current nodes
        return processCommit(info, commitId, service);
    }

    private NodeGetInfo processCommit(NodeGetInfo info, String commitId, NodeService service) {
        Optional<Commit> commit = commitRepository.findByCommitId(commitId);
        if (!commit.isPresent() ) {
            throw new BadRequestException("commitId is invalid");
        }
        Instant time = commit.get().getTimestamp(); //time of commit
        List<String> refCommitIds = null; //get it later if needed
        for (String nodeId : info.getReqElementMap().keySet()) {
            if (!existingNodeContainsNodeId(info, nodeId)) { // nodeId not found
                continue;
            }
            ElementJson indexElement = info.getExistingElementMap().get(nodeId);
            if (indexElement == null) {
                Node n = info.getExistingNodeMap().get(nodeId);
                logger.warn("node db and index mismatch on element commit get: nodeId: " + nodeId +
                    ", docId not found: " + n.getDocId());
                Optional<Commit> last = commitRepository.findByCommitId(n.getLastCommit());
                Optional<Commit> first = commitRepository.findByCommitId(n.getInitialCommit());
                if (!last.isPresent() || !first.isPresent()) {
                    rejectNotFound(info, nodeId);
                    continue;
                }
                indexElement = new ElementJson().setId(nodeId).setDocId(n.getDocId());
                indexElement.setModified(formatter.format(last.get().getTimestamp()));
                indexElement.setModifier(last.get().getCreator());
                indexElement.setCommitId(last.get().getCommitId());
                indexElement.setCreator(first.get().getCreator());
                indexElement.setCreated(formatter.format(first.get().getTimestamp()));
            }
            Instant modified = Instant.from(formatter.parse(indexElement.getModified()));
            Instant created = Instant.from(formatter.parse(indexElement.getCreated()));

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
                    formatter.format(time), refCommitIds);
                if (e.isPresent()) { // found version of element at commit time
                    //TODO determine if element was deleted at the time?
                    addActiveElement(info, nodeId, e.get());
                } else {
                    rejectNotFound(info, nodeId); // element not found at commit time
                }
            } else if (info.getExistingNodeMap().get(nodeId).isDeleted()) { // latest element is before commit, but deleted
                rejectDeleted(info, nodeId, indexElement);
            } else { // latest element version is version at commit, not deleted
                addActiveElement(info, nodeId, indexElement);
            }
        }
        return info;
    }

    private void addActiveElement(NodeGetInfo info, String nodeId, ElementJson indexElement) {
        if(!validateOperation(Operation.GET, null, indexElement, false)) {
            info.addRejection(nodeId, new Rejection(nodeId, 406, "Invalid operation"));
            return;
        }
        info.getActiveElementMap().put(nodeId, indexElement);
    }

    public NodeGetInfo processGetJson(List<ElementJson> elements, Instant time, NodeService service) {
        Optional<Branch> ref = branchRepository.findByBranchId(getContext().getBranchId());
        if (ref.isPresent()) {
            Optional<Commit> c = commitRepository.findLatestByRef(ref.get());
            if (c.isPresent()) {
                return processGetJson(elements, c.get().getCommitId(), service);
            } else {
                throw new BadRequestException("invalid time");
            }
        }
        return null;
    }

    public List<ElementJson> processGetAll() {
        Set<String> indexIds = new HashSet<>();
        List<Node> existingNodes = nodeRepository.findAllByDeleted(false);
        for (Node node : existingNodes) {
            indexIds.add(node.getDocId());
        }
        return nodeIndex.findAllById(indexIds);
    }

    public List<ElementJson> processGetAll(String commitId, NodeService service) {
        if (commitId == null || commitId.isEmpty()) {
            return processGetAll();
        } else {
            List<Node> nodes = nodeRepository.findAll();
            List<ElementJson> el = nodes.stream().map(
                node -> new ElementJson().setId(node.getNodeId())).collect(Collectors.toList());
            NodeGetInfo info = processGetJson(el, commitId, service);
            return new ArrayList<>(info.getActiveElementMap().values());
        }
    }

    public List<ElementJson> processGetAll(Instant time, NodeService service) {
        List<ElementJson> result = new ArrayList<>();
        Optional<Branch> ref = branchRepository.findByBranchId(getContext().getBranchId());
        if (ref.isPresent()) {
            Optional<Commit> c = commitRepository.findByRefAndTimestamp(ref.get(), time);
            if (c.isPresent()) {
                result.addAll(processGetAll(c.get().getCommitId(), service));
            } else {
                throw new BadRequestException("invalid time");
            }
        }
        return result;
    }

    protected void rejectDeleted(NodeGetInfo info, String nodeId, ElementJson indexElement) {
        info.addRejection(nodeId, new Rejection(indexElement, 410, "Element deleted"));
    }

    protected List<String> getRefCommitIds(Instant time) {
        List<String> commitIds = new ArrayList<>();

        Optional<Branch> ref = branchRepository.findByBranchId(getContext().getBranchId());
        ref.ifPresent(current -> {
            List<Commit> refCommits = commitRepository.findByRefAndTimestampAndLimit(current, time, 0);
            for (Commit c : refCommits) {
                commitIds.add(c.getCommitId());
            }
        });
        return commitIds;
    }
}