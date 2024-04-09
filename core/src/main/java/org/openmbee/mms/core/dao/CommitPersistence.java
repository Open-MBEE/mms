package org.openmbee.mms.core.dao;

import org.openmbee.mms.json.CommitJson;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CommitPersistence {

    CommitJson save(CommitJson commitJson, Instant now);

    CommitJson update(CommitJson commitJson);

    Optional<CommitJson> findById(String projectId, String commitId);

    List<CommitJson> findAllById(String projectId, Set<String> commitIds);

    List<CommitJson> findAllByProjectId(String projectId);

    Optional<CommitJson> findLatestByProjectAndRef(String projectId, String refId);

    List<CommitJson> findByProjectAndRefAndTimestampAndLimit(String projectId, String refId, Instant timestamp, int limit);

    List<CommitJson> elementHistory(String projectId, String refId, String elementId);

    Optional<CommitJson> deleteById(String projectId, String commitId);

}
