package org.openmbee.mms.core.services;

import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;

import java.util.Map;

public interface NodeGetInfo {

    CommitJson getCommitJson();

    NodeGetInfo setCommitJson(CommitJson commitJson);

    Map<String, ElementJson> getReqElementMap();

    NodeGetInfo setReqElementMap(Map<String, ElementJson> reqElementMap);

    Map<String, ElementJson> getActiveElementMap();

    NodeGetInfo setActiveElementMap(Map<String, ElementJson> activeElementMap);

    Map<String, ElementJson> getExistingElementMap();

    NodeGetInfo setExistingElementMap(Map<String, ElementJson> existingElementMap);

    Map<String, Rejection> getRejected();

    NodeGetInfo setRejected(Map<String, Rejection> rejected);

    void addRejection(String id, Rejection rejection);

    void setRefId(String refId);
    String getRefId();

}
