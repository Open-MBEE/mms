package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.core.dao.*;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.DeletedException;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.objects.RefsResponse;
import org.openmbee.mms.data.dao.*;
import org.openmbee.mms.data.domains.global.Project;
import org.openmbee.mms.data.domains.scoped.Branch;
import org.openmbee.mms.data.domains.scoped.Commit;
import org.openmbee.mms.json.RefJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class FederatedBranchPersistence implements BranchPersistence {
    private static Logger logger = LoggerFactory.getLogger(FederatedBranchPersistence.class);

    private BranchDAO branchDAO;
    private BranchGDAO branchGDAO;
    private BranchIndexDAO branchIndexDAO;
    private CommitDAO commitDAO;
    private ProjectDAO projectDAO;

    @Autowired
    public FederatedBranchPersistence(BranchDAO branchDAO, BranchGDAO branchGDAO, BranchIndexDAO branchIndexDAO, CommitDAO commitDAO, ProjectDAO projectDAO) {
        this.branchDAO = branchDAO;
        this.branchGDAO = branchGDAO;
        this.branchIndexDAO = branchIndexDAO;
        this.commitDAO = commitDAO;
        this.projectDAO = projectDAO;
    }

    @Override
    public RefJson save(RefJson refJson) {
        //Master branch special case
        boolean isMasterBranch = refJson.getId().equals(Constants.MASTER_BRANCH);

        //Fill in docId
        if (refJson.getDocId() == null || refJson.getDocId().isEmpty()) {
            refJson.setDocId(branchIndexDAO.createDocId(refJson));
        }

        //Setup scoped Branch object
        Branch scopedBranch = new Branch();
        scopedBranch.setBranchId(refJson.getId());
        scopedBranch.setBranchName(refJson.getName());
        scopedBranch.setDescription(refJson.getDescription());
        scopedBranch.setTag(refJson.isTag());
        scopedBranch.setTimestamp(Formats.FORMATTER.parse(refJson.getCreated(), Instant::from));
        scopedBranch.setParentRefId(refJson.getParentRefId());
        scopedBranch.setDocId(refJson.getDocId());
        scopedBranch.setDeleted(Boolean.parseBoolean(refJson.getIsDeleted()));

        //Setup global Branch object
        Optional<Project> project = projectDAO.findByProjectId(refJson.getProjectId());
        if(project.isEmpty()) {
            throw new NotFoundException("project not found");
        }
        org.openmbee.mms.data.domains.global.Branch globalBranch = new org.openmbee.mms.data.domains.global.Branch();
        globalBranch.setProject(project.get());
        globalBranch.setBranchId(refJson.getId());
        globalBranch.setInherit(true);

        //Master branch case. Can skip parent branch and parent commit check
        if (!isMasterBranch) {
            //Validate parent branch
            Optional<Branch> refOption = branchDAO.findByBranchId(scopedBranch.getParentRefId());
            if (!refOption.isPresent()) {
                throw new InternalErrorException("Cannot determine parent branch.");
            }

            //Validate parent commit
            Optional<Commit> parentCommit = commitDAO.findByCommitId(refJson.getParentCommitId());
            if (!parentCommit.isPresent()) {
                throw new InternalErrorException("Cannot determine parent commit.");
            }
            parentCommit.ifPresent(parent -> scopedBranch.setParentCommit(parent.getId()));
        }

        //DO save
        ContextHolder.setContext(null);
        branchGDAO.save(globalBranch);

        String projectId = refJson.getProjectId();
        ContextHolder.setContext(projectId);
        branchIndexDAO.update(refJson);
        branchDAO.save(scopedBranch);
        return refJson;
    }

    @Override
    public RefJson update(RefJson refJson) {
        ContextHolder.setContext(refJson.getProjectId());
        Optional<Branch> existing = branchDAO.findByBranchId(refJson.getId());
        existing.get().setDeleted(refJson.isDeleted());
        branchDAO.save(existing.get());
        branchIndexDAO.update(refJson);
        return refJson;
    }

    @Override
    public List<RefJson> findAll(String projectId) {
        ContextHolder.setContext(projectId);
        List<Branch> branches = branchDAO.findAll();
        Set<String> docIds = new HashSet<>();
        branches.forEach(branch -> docIds.add(branch.getDocId()));
        return branchIndexDAO.findAllById(docIds);
    }

    @Override
    public Optional<RefJson> findById(String projectId, String refId) {
        ContextHolder.setContext(projectId);
        Optional<Branch> branchesOption = this.branchDAO.findByBranchId(refId);
        if (!branchesOption.isPresent()) {
            return Optional.empty();
        }
        Branch b = branchesOption.get();
        if (b.isDeleted()) {
            throw new DeletedException(new RefsResponse());
        }
        Optional<RefJson> refOption = branchIndexDAO.findById(b.getDocId());
        if (!refOption.isPresent()) {
            logger.error("Federated data inconsistency: JSON Not found for {} with docId: {}",
                b.getBranchId(), b.getDocId());
            throw new NotFoundException(new RefsResponse());
        }
        return refOption;
    }

    @Override
    public Optional<RefJson> deleteById(String projectId, String refId) {
        ContextHolder.setContext(projectId);
        Optional<Branch> branch = this.branchDAO.findByBranchId(refId);
        if (!branch.isPresent()) {
            return Optional.empty();
        }
        Branch b = branch.get();
        b.setDeleted(true);
        branchDAO.save(b);
        RefJson refJson = new RefJson().setDocId(b.getDocId()).setDeleted(true)
            .setProjectId(projectId).setId(refId);
        return Optional.of(branchIndexDAO.update(refJson));
    }

    @Override
    public boolean inheritsPermissions(String projectId, String branchId) {
        Optional<org.openmbee.mms.data.domains.global.Branch> branch =
                branchGDAO.findByProject_ProjectIdAndBranchId(projectId, branchId);
        return branch.map(org.openmbee.mms.data.domains.global.Branch::isInherit).orElse(false);
    }
}
