package org.openmbee.mms.core.services;

import java.util.HashMap;
import java.util.Map;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;

public class NodeGetInfoImpl  implements NodeGetInfo {

    private Map<String, ElementJson> reqElementMap;

    // string is node id
    // map of element json objects
    private Map<String, ElementJson> existingElementMap;

    private Map<String, ElementJson> activeElementMap;

    private Map<String, Rejection> rejected;

    private CommitJson commitJson;

    private String refId;

    @Override
    public Map<String, ElementJson> getReqElementMap() {
        return reqElementMap;
    }

    @Override
    public NodeGetInfo setReqElementMap(Map<String, ElementJson> reqElementMap) {
        this.reqElementMap = reqElementMap;
        return this;
    }

    @Override
    public Map<String, ElementJson> getActiveElementMap() {
        return activeElementMap;
    }

    @Override
    public NodeGetInfo setActiveElementMap(
        Map<String, ElementJson> activeElementMap) {
        this.activeElementMap = activeElementMap;
        return this;
    }

    @Override
    public Map<String, ElementJson> getExistingElementMap() {
        return existingElementMap;
    }

    @Override
    public NodeGetInfo setExistingElementMap(
        Map<String, ElementJson> existingElementMap) {
        this.existingElementMap = existingElementMap;
        return this;
    }

    @Override
    public Map<String, Rejection> getRejected() {
        return rejected;
    }

    @Override
    public NodeGetInfo setRejected(Map<String, Rejection> rejected) {
        this.rejected = rejected;
        return this;
    }

    @Override
    public void addRejection(String id, Rejection rejection) {
        if (this.rejected == null) {
            this.rejected = new HashMap<>();
        }
        this.rejected.put(id, rejection);
    }

    @Override
    public CommitJson getCommitJson() {
        return commitJson;
    }

    @Override
    public NodeGetInfo setCommitJson(CommitJson commitJson) {
        this.commitJson = commitJson;
        return this;
    }


    @Override
    public void setRefId(String refId) {
        this.refId = refId;
    }

    @Override
    public String getRefId() {
        return this.refId;
    }
}
