package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.data.dao.BranchDAO;
import org.openmbee.mms.data.dao.CommitDAO;
import org.openmbee.mms.data.dao.CommitIndexDAO;
import org.openmbee.mms.core.dao.CommitPersistence;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.data.domains.scoped.Branch;
import org.openmbee.mms.data.domains.scoped.Commit;
import org.openmbee.mms.data.domains.scoped.CommitType;
import org.openmbee.mms.json.BaseJson;
import org.openmbee.mms.json.CommitJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component("federatedCommitPersistence")
public class FederatedCommitPersistence implements CommitPersistence {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private CommitDAO commitDAO;
    private CommitIndexDAO commitIndexDAO;
    private BranchDAO branchDAO;

    @Autowired
    public FederatedCommitPersistence(CommitDAO commitDAO, CommitIndexDAO commitIndexDAO, BranchDAO branchDAO) {
        this.commitDAO = commitDAO;
        this.commitIndexDAO = commitIndexDAO;
        this.branchDAO = branchDAO;
    }

    @Override
    public CommitJson save(CommitJson commitJson, Instant now) {
        ContextHolder.setContext(commitJson.getProjectId());
        Commit commit = new Commit();
        commit.setComment(commitJson.getComment());
        commit.setCommitId(commitJson.getId());
        commit.setCreator(commitJson.getCreator());
        commit.setBranchId(commitJson.getRefId());
        commit.setCommitType(CommitType.COMMIT);
        commit.setTimestamp(now);

        try {
            commitDAO.save(commit);
            commitIndexDAO.index(commitJson);
            return commitJson;
        } catch (Exception e) {
            logger.error("Couldn't create commit: {}", commitJson.getId(), e);
            //Need to clean up in case of partial creation
            deleteById(commitJson.getProjectId(), commitJson.getId());
            throw new InternalErrorException("Could not create commit");
        }
    }

    @Override
    public CommitJson update(CommitJson commitJson) {
        //TODO need to work on this logic
        ContextHolder.setContext(commitJson.getProjectId());
        Optional<Commit> commitOptional = commitDAO.findByCommitId(commitJson.getId());

        if (commitOptional.isPresent()) {
            Instant now = Instant.now();
            commitJson.setModified(Formats.FORMATTER.format(now));
            commitJson.setDocId(commitJson.getId());
            Commit commit = commitOptional.get();
            commit.setComment(commitJson.getComment());
            //commit.setTimestamp(now);
            commitDAO.save(commit);
            return commitIndexDAO.update(commitJson);
        }
        throw new NotFoundException("Could not update commit");
    }

    @Override
    public Optional<CommitJson> findById(String projectId, String commitId) {
        ContextHolder.setContext(projectId);
        Optional<Commit> commit = commitDAO.findByCommitId(commitId);
        if(!commit.isPresent()) {
            return Optional.empty();
        }
        Optional<CommitJson> commitJson = commitIndexDAO.findById(commit.get().getCommitId());
        if(!commitJson.isPresent()) {
            throw new InternalErrorException(
                String.format("Federated data model inconsistency: Could not find commit json for docId %s",
                    commit.get().getCommitId()));
        }
        return commitJson;
    }

    @Override
    public List<CommitJson> findAllById(String projectId, Set<String> commitIds) {
        ContextHolder.setContext(projectId);
        Set<String> foundCommitIds = new HashSet<>();

        commitIds.forEach(commitId -> {
            Optional<Commit> commitOptional = commitDAO.findByCommitId(commitId);
            if (commitOptional.isPresent()) {
                foundCommitIds.add(commitOptional.get().getCommitId());
            }
        });
        List<CommitJson> commits = commitIndexDAO.findAllById(foundCommitIds);
        commits.sort(Comparator.comparing(CommitJson::getCreated).reversed());
        return commits;
    }

    @Override
    public List<CommitJson> findAllByProjectId(String projectId) {
        ContextHolder.setContext(projectId);
        Set<String> commitIds = new HashSet<>();
        commitDAO.findAll().forEach(commit -> commitIds.add(commit.getCommitId()));
        List<CommitJson> commits = commitIndexDAO.findAllById(commitIds);
        commits.sort(Comparator.comparing(CommitJson::getCreated).reversed());
        return commits;
    }

    @Override
    public Optional<CommitJson> findLatestByProjectAndRef(String projectId, String refId) {
        ContextHolder.setContext(projectId);
        Optional<Branch> branch = branchDAO.findByBranchId(refId);
        if(!branch.isPresent()) {
            return Optional.empty();
        }
        Optional<Commit> commit = commitDAO.findLatestByRef(branch.get());
        if(!commit.isPresent()) {
            return Optional.empty();
        }
        Optional<CommitJson> commitJson = commitIndexDAO.findById(commit.get().getCommitId());
        if(!commitJson.isPresent()) {
            throw new InternalErrorException(
                String.format("Federated data model inconsistency: Could not find commit json for commitId %s",
                    commit.get().getCommitId()));
        }
        return commitJson;
    }

    @Override
    public List<CommitJson> findByProjectAndRefAndTimestampAndLimit(String projectId, String refId, Instant timestamp, int limit) {
        ContextHolder.setContext(projectId);
        Set<String> commitIds = new HashSet<>();
        Optional<Branch> branchOptional = branchDAO.findByBranchId(refId);
        if(!branchOptional.isPresent()) {
            return new ArrayList<>();
        }
        Branch b = branchOptional.get();
        List<Commit> commitList = commitDAO.findByRefAndTimestampAndLimit(b, timestamp, limit);
        commitList.forEach(commit -> commitIds.add(commit.getCommitId()));
        List<CommitJson> commits = commitIndexDAO.findAllById(commitIds);
        commits.sort(Comparator.comparing(CommitJson::getCreated).reversed());
        return commits;
    }

    @Override
    public List<CommitJson> elementHistory(String projectId, String refId, String elementId) {
        ContextHolder.setContext(projectId);
        Optional<Branch> branchOptional = branchDAO.findByBranchId(refId);
        if(!branchOptional.isPresent()) {
            return new ArrayList<>();
        }
        Branch b = branchOptional.get();
        List<Commit> commitList = commitDAO.findByRefAndTimestampAndLimit(b, null, 0);
        Set<String> commitIds = new HashSet<>();
        for (Commit commit: commitList) {
            commitIds.add(commit.getCommitId());
        }
        List<CommitJson> commits = commitIndexDAO.elementHistory(elementId, commitIds);
        commits.sort(Comparator.comparing(CommitJson::getCreated).reversed());
        return commits;
    }

    @Override
    public Optional<CommitJson> deleteById(String projectId, String commitId) {
        ContextHolder.setContext(projectId);
        Optional<Commit> commitOptional = commitDAO.findByCommitId(commitId);
        try {
            Optional<CommitJson> commitJsonOptional =  commitOptional.isPresent() ?
                commitIndexDAO.findById(commitOptional.get().getCommitId()) : Optional.empty();
            if (commitJsonOptional.isPresent()) {
                commitIndexDAO.deleteById(commitJsonOptional.get().getDocId());
                return commitJsonOptional;
            }
        } catch (Exception e) {
            throw new InternalErrorException("Could not delete commit");
        }
        throw new NotFoundException("Could not delete commit");
    }
}
