package org.openmbee.mms.rdb.repositories;

import java.util.Optional;

import org.openmbee.mms.data.dao.BranchGDAO;
import org.openmbee.mms.data.domains.global.Branch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class BranchGDAOImpl implements BranchGDAO {

    private BranchRepository branchRepository;

    @Autowired
    public void setBranchRepository(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Override
    public Optional<Branch> findByProject_ProjectIdAndBranchId(
        String projectId, String branchId) {
        return branchRepository.findByProject_ProjectIdAndBranchId(projectId, branchId);
    }

    @Override
    public Branch save(Branch branch) {
        return branchRepository.save(branch);
    }
}
