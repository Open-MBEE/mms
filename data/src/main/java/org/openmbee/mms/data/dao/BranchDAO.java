package org.openmbee.mms.data.dao;

import java.util.List;
import java.util.Optional;
import org.openmbee.mms.data.domains.scoped.Branch;

public interface BranchDAO {

    Branch save(Branch branch);

    Optional<Branch> findByBranchId(String sysmlid);

    List<Branch> findAll();

    void delete(Branch branch);
}
