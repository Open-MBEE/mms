package org.openmbee.mms.core.dao;

import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.ElementJson;
import org.openmbee.mms.json.RefJson;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

public interface NodePersistence {


    NodeChangeInfo prepareChange(CommitJson commitJson, boolean overwrite, boolean preserveTimestamps);

    NodeChangeInfo prepareAddsUpdates(NodeChangeInfo nodeChangeInfo, Collection<ElementJson> elements);

    NodeChangeInfo prepareDeletes(NodeChangeInfo nodeChangeInfo, Collection<ElementJson> jsons);

    NodeChangeInfo commitChanges(NodeChangeInfo nodeChangeInfo);

    NodeGetInfo findById(String projectId, String refId, String commitId, String elementId);

    List<ElementJson> findAllByNodeType(String projectId, String refId, String commitId, int nodeType, Boolean deleted);

    NodeGetInfo findAll(String projectId, String refId, String commitId, List<ElementJson> elements);

    List<ElementJson> findAll(String projectId, String refId, String commitId, Boolean deleted);

    void streamAllAtCommit(String projectId, String refId, String commitId, OutputStream outputStream, String separator, Boolean deleted);

    void branchElements(RefJson parentBranch, CommitJson parentCommit, RefJson targetBranch);
}
