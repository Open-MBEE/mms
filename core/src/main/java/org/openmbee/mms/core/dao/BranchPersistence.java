package org.openmbee.mms.core.dao;

import org.openmbee.mms.json.RefJson;

import java.util.List;
import java.util.Optional;

public interface BranchPersistence {

    RefJson save(RefJson refJson);

    RefJson update(RefJson refJson);

    List<RefJson> findAll(String projectId);

    Optional<RefJson> findById(String projectId, String refId);

    Optional<RefJson> deleteById(String projectId, String refId);

    boolean inheritsPermissions(String projectId, String branchId);
}
