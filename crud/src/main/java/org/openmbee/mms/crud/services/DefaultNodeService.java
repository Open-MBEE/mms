package org.openmbee.mms.crud.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.objects.ElementsCommitResponse;
import org.openmbee.mms.core.objects.EventObject;
import org.openmbee.mms.core.services.EventService;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.data.domains.scoped.Commit;
import org.openmbee.mms.data.domains.scoped.CommitType;
import org.openmbee.mms.data.domains.scoped.Node;
import org.openmbee.mms.core.dao.CommitDAO;
import org.openmbee.mms.core.dao.CommitIndexDAO;
import org.openmbee.mms.core.dao.NodeDAO;
import org.openmbee.mms.core.dao.NodeIndexDAO;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("defaultNodeService")
public class DefaultNodeService implements NodeService {

    @Value("${mms.stream.batch.size:100000}")
    protected int streamLimit;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected NodeDAO nodeRepository;
    protected CommitDAO commitRepository;
    protected NodeIndexDAO nodeIndex;
    //to save to this use base json classes
    protected CommitIndexDAO commitIndex;
    protected ObjectFactory<NodeGetHelper> nodeGetHelperFactory;
    protected ObjectFactory<NodePostHelper> nodePostHelperFactory;
    protected ObjectFactory<NodeDeleteHelper> nodeDeleteHelperFactory;

    protected Collection<EventService> eventPublisher;

    @Autowired
    public void setNodeRepository(NodeDAO nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Autowired
    public void setCommitRepository(CommitDAO commitRepository) {
        this.commitRepository = commitRepository;
    }

    @Autowired
    public void setNodeIndex(NodeIndexDAO nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    @Autowired
    public void setCommitIndex(CommitIndexDAO commitIndex) {
        this.commitIndex = commitIndex;
    }

    @Autowired
    public void setNodePostHelperFactory(ObjectFactory<NodePostHelper> nodePostHelperFactory) {
        this.nodePostHelperFactory = nodePostHelperFactory;
    }

    public NodePostHelper getNodePostHelper() {
        return this.nodePostHelperFactory.getObject();
    }

    @Autowired
    public void setNodeDeleteHelperFactory(ObjectFactory<NodeDeleteHelper> nodeDeleteHelperFactory) {
        this.nodeDeleteHelperFactory = nodeDeleteHelperFactory;
    }

    public NodeDeleteHelper getNodeDeleteHelper() {
        return this.nodeDeleteHelperFactory.getObject();
    }

    @Autowired
    public void setNodeGetHelperFactory(ObjectFactory<NodeGetHelper> nodeGetHelperFactory) {
        this.nodeGetHelperFactory = nodeGetHelperFactory;
    }

    public NodeGetHelper getNodeGetHelper() {
        return this.nodeGetHelperFactory.getObject();
    }

    @Autowired
    public void setEventPublisher(Collection<EventService> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void readAsStream(String projectId, String refId,
        Map<String, String> params, OutputStream stream, String accept) throws IOException {

        String commitId = params.getOrDefault("commitId", null);
        ContextHolder.setContext(projectId, refId);
        List<Node> nodes;
        if (commitId != null && !commitId.isEmpty()) {
            if (!commitRepository.findByCommitId(commitId).isPresent()) {
                throw new BadRequestException("commit id is invalid");
            }
            nodes = nodeRepository.findAll();
        } else {
            nodes = nodeRepository.findAllByDeleted(false);
        }
        String separator = "\n";
        if (!"application/x-ndjson".equals(accept)) {
            stream.write("{\"elements\":[".getBytes(StandardCharsets.UTF_8));
            separator = ",";
        }
        final String sep = separator;
        AtomicInteger counter = new AtomicInteger();
        batches(nodes, streamLimit).forEach(ns -> {
            try {
                if (counter.get() == 0) {
                    counter.getAndIncrement();
                } else {
                    stream.write(sep.getBytes(StandardCharsets.UTF_8));
                }
                Collection<ElementJson> result = getNodeGetHelper().processGetJsonFromNodes(ns, commitId, this)
                    .getActiveElementMap().values();
                stream.write(result.stream().map(this::toJson).collect(Collectors.joining(sep))
                    .getBytes(StandardCharsets.UTF_8));
            } catch (IOException ioe) {
                logger.error("Error writing to stream", ioe);
                throw new InternalErrorException("Error writing to stream.");
            }
        });
        if (!"application/x-ndjson".equals(accept)) {
            stream.write("]}".getBytes(StandardCharsets.UTF_8));
        } else {
            stream.write("\n".getBytes(StandardCharsets.UTF_8));
        }
        stream.close();
    }

    @Override
    public ElementsResponse read(String projectId, String refId, String id,
        Map<String, String> params) {

        if (id != null && !id.isEmpty()) {
            logger.debug("ElementId given: {}", id);

            ElementsRequest req = buildRequest(id);
            return read(projectId, refId, req, params);

        } else {
            // If no id is provided, return all
            logger.debug("No ElementId given");
            ContextHolder.setContext(projectId, refId);

            ElementsResponse response = new ElementsResponse();
            String commitId = params.getOrDefault("commitId", null);
            response.getElements().addAll(getNodeGetHelper().processGetAll(commitId, this));
            return response;
        }
    }

    @Override
    public ElementsResponse read(String projectId, String refId, ElementsRequest req,
        Map<String, String> params) {

        String commitId = params.getOrDefault("commitId", null);
        ContextHolder.setContext(projectId, refId);

        NodeGetInfo info = getNodeGetHelper().processGetJson(req.getElements(), commitId, this);

        ElementsResponse response = new ElementsResponse();
        response.getElements().addAll(info.getActiveElementMap().values());
        response.setRejected(new ArrayList<>(info.getRejected().values()));
        return response;
    }

    @Override
    public ElementsCommitResponse createOrUpdate(String projectId, String refId, ElementsRequest req,
                                                 Map<String, String> params, String user) {

        ContextHolder.setContext(projectId, refId);
        boolean overwriteJson = Boolean.parseBoolean(params.get("overwrite"));
        NodePostHelper nodePostHelper = getNodePostHelper();
        nodePostHelper.setPreserveTimestamps(Boolean.parseBoolean(params.get("preserveTimestamps")));
        String commitId = params.get("commitId");

        NodeChangeInfo info = nodePostHelper
            .processPostJson(req.getElements(), overwriteJson,
                createCommit(user, refId, projectId, req, commitId), this);

        commitChanges(info);

        ElementsCommitResponse response = new ElementsCommitResponse();
        response.getElements().addAll(info.getUpdatedMap().values());
        response.setRejected(new ArrayList<>(info.getRejected().values()));
        if(!info.getUpdatedMap().isEmpty()) {
            response.setCommitId(info.getCommitJson().getId());
        }
        return response;
    }

    public void commitChanges(NodeChangeInfo info) {
        Map<String, Node> nodes = info.getToSaveNodeMap();
        Map<String, ElementJson> json = info.getUpdatedMap();
        CommitJson cmjs = info.getCommitJson();
        Instant now = info.getNow();
        if (!nodes.isEmpty()) {
            try {
                if (json != null && !json.isEmpty()) {
                    this.nodeIndex.indexAll(json.values());
                }
                try { this.nodeIndex.removeFromRef(info.getOldDocIds()); } catch(Exception e) {}
                this.commitIndex.index(cmjs);

                Optional<Commit> existing = this.commitRepository.findByCommitId(cmjs.getId());
                existing.ifPresentOrElse(
                    current -> {
                        this.logger.debug(String.format("Commit object %s already exists. Skipping record creation.", current.getCommitId()));
                    },
                    () -> {
                        Commit commit = new Commit();
                        commit.setCommitId(cmjs.getId());
                        commit.setBranchId(cmjs.getRefId());
                        commit.setCommitType(CommitType.COMMIT);
                        commit.setCreator(cmjs.getCreator());
                        commit.setTimestamp(now);
                        commit.setComment(cmjs.getComment());
                        this.commitRepository.save(commit);
                    });
                this.nodeRepository.saveAll(new ArrayList<>(nodes.values()));
            } catch (Exception e) {
                logger.error("Error in commitChanges: ", e);
                throw new InternalErrorException("Error committing changes: " + e.getMessage());
            }
            eventPublisher.forEach((pub) -> pub.publish(
                EventObject.create(cmjs.getProjectId(), cmjs.getRefId(), "commit", cmjs)));
        }
    }

    @Override
    public void extraProcessPostedElement(ElementJson element, Node node, NodeChangeInfo info) {
    }

    @Override
    public void extraProcessDeletedElement(ElementJson element, Node node, NodeChangeInfo info) {
    }

    @Override
    public void extraProcessGotElement(ElementJson element, Node node, NodeGetInfo info) {
    }

    @Override
    public ElementsCommitResponse delete(String projectId, String refId, String id, String user) {
        ElementsRequest req = buildRequest(id);
        return delete(projectId, refId, req, user);
    }

    protected ElementsRequest buildRequest(String id) {
        ElementsRequest req = new ElementsRequest();
        List<ElementJson> list = new ArrayList<>();
        list.add(new ElementJson().setId(id));
        req.setElements(list);
        return req;
    }

    protected ElementsRequest buildRequest(Collection<String> ids) {
        ElementsRequest req = new ElementsRequest();
        List<ElementJson> list = new ArrayList<>();
        for (String id: ids) {
            list.add(new ElementJson().setId(id));
        }
        req.setElements(list);
        return req;
    }

    @Override
    public ElementsCommitResponse delete(String projectId, String refId, ElementsRequest req, String user) {
        ContextHolder.setContext(projectId, refId);

        NodeChangeInfo info = getNodeDeleteHelper()
            .processDeleteJson(req.getElements(), createCommit(user, refId, projectId, req, null),
                this);
        ElementsCommitResponse response = new ElementsCommitResponse();

        commitChanges(info);

        response.getElements().addAll(info.getDeletedMap().values());
        response.setRejected(new ArrayList<>(info.getRejected().values()));
        if(!info.getDeletedMap().isEmpty()) {
            response.setCommitId(info.getCommitJson().getId());
        }
        return response;
    }

    private CommitJson createCommit(String creator, String refId, String projectId,
        ElementsRequest req, String commitId) {
        CommitJson cmjs = new CommitJson();
        cmjs.setCreator(creator);
        cmjs.setComment(req.getComment());
        cmjs.setSource(req.getSource());
        cmjs.setRefId(refId);
        cmjs.setProjectId(projectId);

        if (commitId != null && !commitId.isEmpty()) {
            cmjs.setId(commitId);
        }

        return cmjs;
    }

    protected static <T> Stream<List<T>> batches(List<T> source, int length) {
        return IntStream.iterate(0, i -> i < source.size(), i -> i + length)
            .mapToObj(i -> source.subList(i, Math.min(i + length, source.size())));
    }

    protected String toJson(ElementJson elementJson) {
        try {
            return objectMapper.writeValueAsString(elementJson);
        } catch (JsonProcessingException e) {
            logger.error("Error in toJson: ", e);
        }
        return "";
    }
}
