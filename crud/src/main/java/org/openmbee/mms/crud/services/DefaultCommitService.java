package org.openmbee.mms.crud.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.dao.*;
import org.openmbee.mms.core.services.CommitService;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.RefJson;
import org.openmbee.mms.core.objects.CommitsRequest;
import org.openmbee.mms.core.objects.CommitsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("defaultCommitService")
public class DefaultCommitService implements CommitService {
    protected CommitPersistence commitPersistence;
    protected BranchPersistence branchPersistence;

    @Autowired
    public void setCommitPersistence(CommitPersistence commitPersistence) {
        this.commitPersistence = commitPersistence;
    }

    @Autowired
    public void setBranchPersistence(BranchPersistence branchPersistence) {
        this.branchPersistence = branchPersistence;
    }

    @Override
    public CommitsResponse getRefCommits(String projectId, String refId, Map<String, String> params) {
        int limit = 0;
        Instant timestamp = null;
        if (params.containsKey("limit")) {
            limit = Integer.parseInt(params.get("limit"));
        }
        if (params.containsKey("maxTimestamp")) {
            try {
                timestamp = new SimpleDateFormat(Formats.DATE_FORMAT).parse(params.get("maxTimestamp")).toInstant();
            } catch (ParseException e) {
                e.printStackTrace();
                throw new BadRequestException("maxTimestamp parse error, use " +
                    Formats.FORMATTER.format(Instant.now()) + " as example");
            }
        }
        Optional<RefJson> ref = branchPersistence.findById(projectId, refId);
        List<CommitJson> resJson = new ArrayList<>();
        CommitsResponse res = new CommitsResponse();

        int finalLimit = limit;
        Instant finalTimestamp = timestamp;

        ref.ifPresentOrElse(branch -> {
            //branch for case that finalTimestamp is null AND/OR finalLimit is 0
            List<CommitJson> commits = commitPersistence.findByProjectAndRefAndTimestampAndLimit(projectId, refId, finalTimestamp, finalLimit);
            resJson.addAll(commits);
        }, () -> {
            throw new NotFoundException("Branch not found");
        });

        res.setCommits(resJson);
        return res;
    }

    @Override
    public CommitsResponse getCommit(String projectId, String commitId) {
        CommitsResponse res = new CommitsResponse();
        Optional<CommitJson> commit = commitPersistence.findById(projectId, commitId);
        if (!commit.isPresent()) {
            throw new NotFoundException("Commit not found");
        }
        res.getCommits().add(commit.get());
        return res;
    }

    @Override
    public CommitsResponse getElementCommits(String projectId, String refId, String elementId, Map<String, String> params) {
        CommitsResponse res = new CommitsResponse();
        Optional<RefJson> ref = branchPersistence.findById(projectId, refId);
        if (!ref.isPresent()) {
            throw new NotFoundException("Branch not found");
        }
        res.getCommits().addAll(commitPersistence.elementHistory(projectId, refId, elementId));
        return res;
    }

    @Override
    public CommitsResponse getCommits(String projectId, CommitsRequest req) {
        Set<String> ids = new HashSet<>();
        for (CommitJson j : req.getCommits()) {
            ids.add(j.getId());
        }
        CommitsResponse res = new CommitsResponse();
        List<CommitJson> commitJsonList = commitPersistence.findAllById(projectId, ids);
        if (commitJsonList.isEmpty()) {
            throw new NotFoundException("Commit not found");
        }

        res.getCommits().addAll(commitJsonList);
        return res;
    }

    @Override
    public boolean isProjectNew(String projectId) {
        // if project is not new, there must be at least 1 commit to master
        List<CommitJson> commits = commitPersistence.findByProjectAndRefAndTimestampAndLimit(projectId, "master", null, 1);
        return commits == null || commits.isEmpty();
    }
}
