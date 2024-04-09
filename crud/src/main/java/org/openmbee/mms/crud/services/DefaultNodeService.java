package org.openmbee.mms.crud.services;

import org.openmbee.mms.core.dao.*;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.ConflictException;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.objects.ElementsCommitResponse;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.core.objects.EventObject;
import org.openmbee.mms.core.services.EventService;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.crud.CrudConstants;
import org.openmbee.mms.json.BaseJson;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


@Service("defaultNodeService")
public class DefaultNodeService implements NodeService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected CommitPersistence commitPersistence;
    protected NodePersistence nodePersistence;

    protected Collection<EventService> eventPublisher;

    @Autowired
    public void setCommitPersistence(CommitPersistence commitPersistence) {
        this.commitPersistence = commitPersistence;
    }

    @Autowired
    public void setNodePersistence(NodePersistence nodePersistence) {
        this.nodePersistence = nodePersistence;
    }

    public NodePersistence getNodePersistence() {
        return nodePersistence;
    }

    @Autowired
    public void setEventPublisher(Collection<EventService> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void readAsStream(String projectId, String refId, Map<String, String> params, OutputStream stream,
            String accept) throws IOException {

        String commitId = params.getOrDefault(CrudConstants.COMMITID, null);

        if (commitId != null && !commitId.isEmpty()) {
            if (!commitPersistence.findById(projectId, commitId).isPresent()) {
                throw new BadRequestException("commit id is invalid");
            }
        } else {
            Optional<CommitJson> commitJson = commitPersistence.findLatestByProjectAndRef(projectId, refId);
            if (!commitJson.isPresent()) {
                throw new InternalErrorException("Could not find latest commit for project and ref");
            }
            commitId = commitJson.get().getId();
        }

        String separator = "\n";
        if (!"application/x-ndjson".equals(accept)) {
            String intro = "{\"commitId\":\"" + commitId + "\",\"elements\":[";
            stream.write(intro.getBytes(StandardCharsets.UTF_8));
            separator = ",";
        }

        nodePersistence.streamAllAtCommit(projectId, refId, commitId, stream, separator);

        if (!"application/x-ndjson".equals(accept)) {
            stream.write("]}".getBytes(StandardCharsets.UTF_8));
        } else {
            stream.write("\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public ElementsResponse read(String projectId, String refId, String id,
        Map<String, String> params) {

        if (id != null && !id.isEmpty()) {
            logger.debug("ElementId given: {}", id);
            ElementsRequest req = buildRequest(id);
            return read(projectId, refId, req, params);
        }
        String commitId = params.getOrDefault(CrudConstants.COMMITID, null);
        if (commitId == null) {
            Optional<CommitJson> commitJson = commitPersistence.findLatestByProjectAndRef(projectId, refId);
            if (!commitJson.isPresent()) {
                throw new InternalErrorException("Could not find latest commit for project and ref");
            }
            commitId = commitJson.get().getId();
        }
        // If no id is provided, return all
        ElementsResponse response = new ElementsResponse();
        logger.debug("No ElementId given");

        List<ElementJson> nodes = nodePersistence.findAll(projectId, refId, commitId);
        response.getElements().addAll(nodes);
        response.getElements().forEach(v -> v.setRefId(refId));
        response.setCommitId(commitId);
        return response;
    }

    @Override
    public ElementsResponse read(String projectId, String refId, ElementsRequest req,
        Map<String, String> params) {

        String commitId = params.getOrDefault(CrudConstants.COMMITID, null);
        if (commitId == null) {
            Optional<CommitJson> commitJson = commitPersistence.findLatestByProjectAndRef(projectId, refId);
            if (!commitJson.isPresent()) {
                throw new InternalErrorException("Could not find latest commit for project and ref");
            }
            commitId = commitJson.get().getId();
        }

        NodeGetInfo info = nodePersistence.findAll(projectId, refId, commitId, req.getElements());

        ElementsResponse response = new ElementsResponse();
        response.getElements().addAll(info.getActiveElementMap().values());
        response.getElements().forEach(v -> v.setRefId(refId));
        response.setRejected(new ArrayList<>(info.getRejected().values()));
        response.setCommitId(commitId);
        return response;
    }

    @Override
    public ElementsCommitResponse createOrUpdate(String projectId, String refId, ElementsRequest req,
            Map<String, String> params, String user) {

        boolean overwriteJson = Boolean.parseBoolean(params.get(CrudConstants.OVERWRITE));
        boolean preserveTimestamps = Boolean.parseBoolean(params.get(CrudConstants.PRESERVETIMESTAMPS));
        String commitId = params.getOrDefault(CrudConstants.COMMITID, null);
        String lastCommitId = req.getLastCommitId();

        if (lastCommitId != null && !lastCommitId.isEmpty()) {
            Optional<CommitJson> latestCommit = commitPersistence.findLatestByProjectAndRef(projectId, refId);
            if (latestCommit.isEmpty() || !lastCommitId.equals(latestCommit.get().getId())) {
                throw new ConflictException("Given commitId " + lastCommitId + " is not the latest");
            }
        }

        NodeChangeInfo changes = nodePersistence.prepareChange(createCommit(user, refId, projectId, commitId, req),
            overwriteJson, preserveTimestamps);
        changes = nodePersistence.prepareAddsUpdates(changes, req.getElements());

        for (ElementJson element : changes.getUpdatedMap().values()) {
            extraProcessPostedElement(changes, element);
        }
        if (req.getDeletes() != null) {
            changes = nodePersistence.prepareDeletes(changes, req.getDeletes());
        }

        changes = nodePersistence.commitChanges(changes);
        CommitJson commitJson = changes.getCommitJson();
        eventPublisher.forEach(pub -> pub.publish(
            EventObject.create(commitJson.getProjectId(), commitJson.getRefId(), CrudConstants.COMMIT_SC, commitJson)));

        ElementsCommitResponse response = new ElementsCommitResponse();
        response.getElements().addAll(changes.getUpdatedMap().values());
        response.setDeleted(new ArrayList<>(changes.getDeletedMap().values()));
        response.setRejected(new ArrayList<>(changes.getRejected().values()));
        if(!changes.getUpdatedMap().isEmpty() || !changes.getDeletedMap().isEmpty()) {
            response.setCommitId(changes.getCommitJson().getId());
        }
        return response;
    }


    //Info is used in one of the child classes
    public void extraProcessPostedElement(NodeChangeInfo info, ElementJson element) {
        if (element.getType() == null || element.getType().isEmpty()) {
            element.setType(CrudConstants.NODE);
        }
    }

    @Override
    public ElementsCommitResponse delete(String projectId, String refId, String id, String user) {
        ElementsRequest req = buildRequest(id);
        return delete(projectId, refId, req, user);
    }

    @Override
    public ElementsCommitResponse delete(String projectId, String refId, ElementsRequest req, String user) {
        NodeChangeInfo changes = nodePersistence.prepareChange(createCommit(user, refId, projectId, null, req),
            false, false);
        changes = nodePersistence.prepareDeletes(changes, req.getElements());


        changes = nodePersistence.commitChanges(changes);
        ElementsCommitResponse response = new ElementsCommitResponse();
        response.getElements().addAll(changes.getDeletedMap().values());
        response.setRejected(new ArrayList<>(changes.getRejected().values()));
        if(!changes.getDeletedMap().isEmpty()) {
            response.setCommitId(changes.getCommitJson().getId());
        }
        return response;
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

    protected ElementsRequest buildRequestFromJsons(Collection<ElementJson> jsons) {
        return buildRequest(jsons.stream().map(BaseJson::getId).collect(Collectors.toList()));
    }

    private CommitJson createCommit(String creator, String refId, String projectId, String commitId, ElementsRequest req) {
        CommitJson cmjs = new CommitJson();
        cmjs.setCreator(creator);
        cmjs.setComment(req.getComment());
        cmjs.setSource(req.getSource());
        cmjs.setRefId(refId);
        cmjs.setProjectId(projectId);
        if (commitId != null) {
            cmjs.setId(commitId);
            cmjs.setDocId(commitId);
        }
        return cmjs;
    }
}
