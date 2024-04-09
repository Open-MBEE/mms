package org.openmbee.mms.crud.services;

import java.util.Collection;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.core.dao.*;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.DeletedException;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.objects.EventObject;
import org.openmbee.mms.core.objects.RefsResponse;
import org.openmbee.mms.core.services.BranchService;
import org.openmbee.mms.core.services.EventService;
import org.openmbee.mms.crud.CrudConstants;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.RefJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class DefaultBranchService implements BranchService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private BranchPersistence branchPersistence;

    private CommitPersistence commitPersistence;

    private NodePersistence nodePersistence;

    protected Collection<EventService> eventPublisher;

    @Autowired
    public void setBranchPersistence(BranchPersistence branchPersistence) {
        this.branchPersistence = branchPersistence;
    }

    @Autowired
    public void setCommitPersistence(CommitPersistence commitPersistence) {
        this.commitPersistence = commitPersistence;
    }

    @Autowired
    public void setNodePersistence(NodePersistence nodePersistence) {
        this.nodePersistence = nodePersistence;
    }

    @Autowired
    public void setEventPublisher(Collection<EventService> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public RefsResponse getBranches(String projectId) {
        RefsResponse branchesResponse = new RefsResponse();
        branchesResponse.setRefs(branchPersistence.findAll(projectId));
        return branchesResponse;
    }

    public RefsResponse getBranch(String projectId, String id) {
        RefsResponse response = new RefsResponse();
        Optional<RefJson> refJsonOptional = branchPersistence.findById(projectId, id);
        if (refJsonOptional.isEmpty()) {
            throw new NotFoundException(response.addMessage("Branch not found"));
        }
        response.getRefs().add(refJsonOptional.get());
        if (refJsonOptional.get().isDeleted()) {
            throw new DeletedException(response);
        }
        return response;
    }

    public RefJson createBranch(String projectId, RefJson branch) {
        logger.info("Saving branch: {}", branch.getId());

        Instant now = Instant.now();
        branch.setCreated(Formats.FORMATTER.format(now));
        branch.setDeleted(false);
        branch.setProjectId(projectId);
        branch.setStatus(CrudConstants.CREATING);

        //Ensure that the type is of Branch
        if (branch.getType() == null || branch.getType().isEmpty()) {
            branch.setType(Constants.BRANCH_TYPE);
        }

        //Find parent branch
        String parentRefId;
        Optional<CommitJson> parentCommit = null;
        if (branch.getParentCommitId() != null && !branch.getParentCommitId().isEmpty()) {
            parentCommit = commitPersistence.findById(projectId, branch.getParentCommitId());
            if (!parentCommit.isPresent()) {
                throw new BadRequestException("Parent commit cannot be determined or found");
            }
            parentRefId = parentCommit.get().getRefId();
            branch.setParentRefId(parentRefId);
        } else if (branch.getParentRefId() != null && !branch.getParentRefId().isEmpty()) {
            parentRefId = branch.getParentRefId();
        } else {
            parentRefId = Constants.MASTER_BRANCH;
            branch.setParentRefId(parentRefId);
        }
        Optional<RefJson> parentRefOption = branchPersistence.findById(projectId, parentRefId);
        if (!parentRefOption.isPresent()) {
            throw new BadRequestException("Parent branch cannot be determined");
        }

        //Find parent commit
        // AND the commit federated are expecting the branches to be in PG
        if (parentCommit == null || !parentCommit.isPresent()) {
            parentCommit = commitPersistence.findLatestByProjectAndRef(projectId, parentRefId);
            parentCommit.ifPresent(parent ->
                branch.setParentCommitId(parent.getId())
            );
        }
        if (!parentCommit.isPresent()) {
            throw new BadRequestException("Parent commit cannot be determined or found");
        }

        //Do branch creation
        try {
            RefJson committedBranch = branchPersistence.save(branch);
            nodePersistence.branchElements(parentRefOption.get(), parentCommit.get(), committedBranch);
            committedBranch.setStatus(CrudConstants.CREATED);
            branchPersistence.update(committedBranch);
            //TODO transaction commit
            eventPublisher.forEach(pub -> pub.publish(
                EventObject.create(projectId, committedBranch.getId(), "branch_created", committedBranch)));
            return committedBranch;
        } catch (Exception e) {
            //TODO transaction rollback
            logger.error("Couldn't create branch: {}", branch.getId(), e);
            throw new InternalErrorException(e);
        }
    }

    @Override
    public RefJson updateBranch(String projectId, RefJson branch) {
        if (projectId != null && branch != null) {
            Optional<RefJson> refJson = branchPersistence.findById(projectId, branch.getId());
            if (!refJson.isPresent()) {
                throw new BadRequestException("Branch: " + branch.getId() + " Not found for project: " + projectId);
            }
            if (refJson.get().isDeleted()) {
                //un-delete action
                branch.setDeleted(false);
            }
        }
       return branchPersistence.update(branch);
    }

    public RefsResponse deleteBranch(String projectId, String id) {
        RefsResponse branchesResponse = new RefsResponse();
        if (Constants.MASTER_BRANCH.equals(id)) {
            throw new BadRequestException(branchesResponse.addMessage("Cannot delete master"));
        }
        Optional<RefJson> branch = branchPersistence.deleteById(projectId, id);
        if (!branch.isPresent()) {
            throw new NotFoundException(branchesResponse);
        }
        branchesResponse.setRefs(branch.map(List::of).orElseGet(List::of));
        return branchesResponse;
    }
}
