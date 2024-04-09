package org.openmbee.mms.data.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.openmbee.mms.json.BaseJson;
import org.openmbee.mms.json.ElementJson;

public interface NodeIndexDAO {

    void indexAll(Collection<? extends BaseJson> jsons);

    void index(BaseJson json);

    Optional<ElementJson> findById(String docId);

    List<ElementJson> findAllById(Set<String> docIds);

    void deleteById(String docId);

    void deleteAll(Collection<? extends BaseJson> jsons);

    boolean existsById(String docId);

    Optional<ElementJson> getByCommitId(String commitId, String nodeId);

    Optional<ElementJson> getElementLessThanOrEqualTimestamp(String nodeId, String timestamp,
        List<String> refsCommitIds);

    void removeFromRef(Set<String> docIds);

    void addToRef(Set<String> docIds);

}
