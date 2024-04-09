package org.openmbee.mms.data.dao;

import java.util.Optional;
import org.openmbee.mms.data.domains.global.Branch;

public interface BranchGDAO {

    Optional<Branch> findByProject_ProjectIdAndBranchId(String projectId, String branchId);

    Branch save(Branch branch);
}
