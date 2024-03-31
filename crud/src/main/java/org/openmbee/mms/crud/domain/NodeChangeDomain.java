package org.openmbee.mms.crud.domain;

import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeChangeInfoImpl;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.json.BaseJson;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public abstract class NodeChangeDomain extends JsonDomain {
    private static final Logger logger = LoggerFactory.getLogger(NodeChangeDomain.class);

    private NodeGetDomain nodeGetDomain;
    private CommitDomain commitDomain;

    private List<NodeUpdateFilter> nodeUpdateFilters;

    public NodeChangeDomain(NodeGetDomain nodeGetDomain, CommitDomain commitDomain, List<NodeUpdateFilter> nodeUpdateFilters) {
        this.nodeGetDomain = nodeGetDomain;
        this.commitDomain = commitDomain;
        this.nodeUpdateFilters = nodeUpdateFilters;
    }

    public NodeChangeInfo initInfo(CommitJson commitJson, boolean overwrite, boolean preserveTimestamps) {

        NodeChangeInfo info = (NodeChangeInfo) nodeGetDomain.initInfo(commitJson, this::createNodeChangeInfo);

        Instant now = Instant.now();
        if (commitJson != null) {
            commitDomain.initCommitJson(commitJson, now);
        }

        info.setUpdatedMap(new HashMap<>());
        info.setDeletedMap(new HashMap<>());
        info.setInstant(now);
        info.setOverwrite(overwrite);
        info.setPreserveTimestamps(preserveTimestamps);

        return info;
    }


    public void processElementAdded(NodeChangeInfo info, ElementJson element) {
        CommitJson commitJson = info.getCommitJson();
        processElementAddedOrUpdated(info, element);

        element.setCreator(commitJson.getCreator()); //Only set on creation of new element
        element.setCreated(commitJson.getCreated());
    }

    public boolean processElementUpdated(NodeChangeInfo info, ElementJson element, ElementJson existing) {
        if (nodeUpdateFilters.stream().anyMatch(f -> !f.filterUpdate(info, element, existing))) {
            return false;
        }
        processElementAddedOrUpdated(info, element);
        return true;
    }

    protected void processElementAddedOrUpdated(NodeChangeInfo info, ElementJson element) {
        CommitJson commitJson = info.getCommitJson();
        element.setProjectId(commitJson.getProjectId());
        element.setRefId(commitJson.getRefId());
        List<String> inRefIds = new ArrayList<>();
        inRefIds.add(commitJson.getRefId());
        element.setInRefIds(inRefIds);
        element.setCommitId(commitJson.getId());

        if(!info.getPreserveTimestamps()) {
            element.setModified(commitJson.getCreated());
            element.setModifier(commitJson.getCreator());
        }

        info.getUpdatedMap().put(element.getId(), element);
    }

    public void processElementDeleted(NodeChangeInfo info, ElementJson element) {
    }

    public NodeChangeInfo processDeleteJson(NodeChangeInfo info, Collection<ElementJson> elements) {
        for (ElementJson element : elements) {
            ElementJson request = info.getReqElementMap().get(element.getId());
            request.putAll(element);
            processElementDeleted(info, request);
            info.getDeletedMap().put(element.getId(), request);
        }
        return info;
    }

    protected NodeChangeInfo createNodeChangeInfo() {
        return new NodeChangeInfoImpl();
    }

    protected void rejectNotFound(NodeGetInfo info, String elementId) {
        info.addRejection(elementId, new Rejection(elementId, 404, "Not Found"));
    }


    // create new elastic id for all element json, update modified time, modifier (use dummy for now), set _projectId, _refId, _inRefIds
    public abstract NodeChangeInfo processPostJson(NodeChangeInfo nodeChangeInfo, Collection<ElementJson> elements);



    public abstract void primeNodeChangeInfo(NodeChangeInfo nodeChangeInfo, Collection<ElementJson> transactedElements);
}
