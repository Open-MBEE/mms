package org.openmbee.mms.crud.services;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.openmbee.mms.core.dao.BranchPersistence;
import org.openmbee.mms.core.dao.CommitPersistence;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.objects.CommitsRequest;
import org.openmbee.mms.core.objects.CommitsResponse;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.RefJson;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class DefaultCommitServiceTest {

    @Spy
    @InjectMocks
    private DefaultCommitService defaultCommitService;

    @Mock
    private CommitPersistence commitPersistence;
    @Mock
    private BranchPersistence branchPersistence;



    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        defaultCommitService.setCommitPersistence(commitPersistence);
        defaultCommitService.setBranchPersistence(branchPersistence);
    }


    @Test
    public void getRefCommitsBranchNotFound() {
        String projectId = "PROJECT_ID";
        String refId = "REF_ID";

        Map<String, String> params = new HashMap<>();
        params.put("limit", Integer.toString(1));
        params.put("maxTimestamp", "2026-07-26T12:06:01.072-0400");

        when(branchPersistence.findById(projectId, refId)).thenReturn(Optional.empty());

        try {
            defaultCommitService.getRefCommits(projectId, refId, params);
        } catch (NotFoundException e) {
            assertNotNull(e);
        }

        resetMocks();
    }

    @Test
    public void getRefCommitsBadParams() {
        String projectId = "PROJECT_ID";
        String refId = "REF_ID";

        Map<String, String> params = new HashMap<>();
        params.put("limit", Integer.toString(1));
        params.put("maxTimestamp", "1776-07-04");

        when(branchPersistence.findById(projectId, refId)).thenReturn(Optional.empty());

        try {
            defaultCommitService.getRefCommits(projectId, refId, params);
        } catch (BadRequestException e) {
            assertNotNull(e);
        }

        resetMocks();
    }


    @Test
    public void getRefCommits() {
        String projectId = "PROJECT_ID";
        String refId = "REF_ID";

        Map<String, String> params = new HashMap<>();
        params.put("limit", Integer.toString(1));
        params.put("maxTimestamp", "2026-07-26T12:06:01.072-0400");

        CommitJson testCommit = new CommitJson();
        testCommit.setCommitId("TEST_COMMIT_ID");
        testCommit.setCreator("TEST_CREATOR");
        testCommit.setCreated("TEST_CREATED");
        testCommit.setDocId("TEST_DOC_ID");
        testCommit.setComment("TEST_COMMENT");

        List<CommitJson> testCommitList = new ArrayList();
        testCommitList.add(testCommit);

        RefJson refJson = new RefJson();
        when(branchPersistence.findById(projectId, refId)).thenReturn(Optional.of(refJson));

        when(commitPersistence.findByProjectAndRefAndTimestampAndLimit(eq(projectId), eq(refId), any(), anyInt())).thenReturn(testCommitList);

        CommitsResponse response = defaultCommitService.getRefCommits(projectId, refId, params);

        assertFalse(response.getCommits().isEmpty());

        resetMocks();
    }

    @Test
    public void getCommitCommitNotFound() {
        String projectId = "PROJECT_ID";
        String commitId = "COMMIT_ID";

        when(commitPersistence.findById(projectId, commitId)).thenReturn(Optional.empty());

        try {
            defaultCommitService.getCommit(projectId, commitId);
        } catch (NotFoundException e) {
            assertNotNull(e);
        }

        resetMocks();
    }

    @Test
    public void getCommit() {
        String projectId = "PROJECT_ID";
        String commitId = "COMMIT_ID";

        CommitJson testCommit = new CommitJson();
        testCommit.setCommitId(commitId);

        when(commitPersistence.findById(projectId, commitId)).thenReturn(Optional.of(testCommit));

        CommitsResponse response = defaultCommitService.getCommit(projectId, commitId);

        assertFalse(response.getCommits().isEmpty());

        resetMocks();
    }

    @Test
    public void getElementCommitsBranchNotFound() {
        String projectId = "PROJECT_ID";
        String refId = "REF_ID";
        String elementId = "ELEMENT_ID";

        Map<String, String> params = new HashMap<>();
        params.put("limit", Integer.toString(1));
        params.put("maxTimestamp", "2026-07-26T12:06:01.072-0400");

        when(branchPersistence.findById(projectId, refId)).thenReturn(Optional.empty());

        try {
            defaultCommitService.getElementCommits(projectId, refId, elementId, params);
        } catch (NotFoundException e) {
            assertNotNull(e);
        }

        resetMocks();
    }

    @Test
    public void getElementCommitsCommitsNotFound() {
        String projectId = "PROJECT_ID";
        String refId = "REF_ID";
        String elementId = "ELEMENT_ID";

        Map<String, String> params = new HashMap<>();
        params.put("limit", Integer.toString(1));
        params.put("maxTimestamp", "2026-07-26T12:06:01.072-0400");

        RefJson refJson = new RefJson();
        when(branchPersistence.findById(projectId, refId)).thenReturn(Optional.of(refJson));

        when(commitPersistence.findByProjectAndRefAndTimestampAndLimit(projectId, refId, null, 0)).thenReturn(new ArrayList<>());
        when(commitPersistence.elementHistory(eq(projectId), eq(elementId), any())).thenReturn(new ArrayList<>());

        CommitsResponse response = defaultCommitService.getElementCommits(projectId, refId, elementId, params);

        assertTrue(response.getCommits().isEmpty());

        resetMocks();
    }

    @Test
    public void getElementCommits() {
        String projectId = "PROJECT_ID";
        String refId = "REF_ID";
        String elementId = "ELEMENT_ID";

        Map<String, String> params = new HashMap<>();
        params.put("limit", Integer.toString(1));
        params.put("maxTimestamp", "2026-07-26T12:06:01.072-0400");

        CommitJson testCommit = new CommitJson();
        testCommit.setCommitId("TEST_COMMIT_ID");
        testCommit.setCreator("TEST_CREATOR");
        testCommit.setCreated("TEST_CREATED");
        testCommit.setDocId("TEST_DOC_ID");
        testCommit.setComment("TEST_COMMENT");

        List<CommitJson> testCommitList = new ArrayList();
        testCommitList.add(testCommit);

        RefJson refJson = new RefJson();
        when(branchPersistence.findById(projectId, refId)).thenReturn(Optional.of(refJson));

        when(commitPersistence.findByProjectAndRefAndTimestampAndLimit(projectId, refId, null, 0)).thenReturn(testCommitList);
        when(commitPersistence.elementHistory(eq(projectId), eq(elementId), any())).thenReturn(testCommitList);

        CommitsResponse response = defaultCommitService.getElementCommits(projectId, refId, elementId, params);

        assertFalse(response.getCommits().isEmpty());

        resetMocks();
    }

    @Test
    public void getCommitsThrowsError() {
        String projectId = "PROJECT_ID";

        CommitJson testCommit = new CommitJson();
        testCommit.setCommitId("TEST_COMMIT_ID");
        testCommit.setCreator("TEST_CREATOR");
        testCommit.setCreated("TEST_CREATED");
        testCommit.setDocId("TEST_DOC_ID");
        testCommit.setComment("TEST_COMMENT");

        List<CommitJson> testCommitList = new ArrayList();
        testCommitList.add(testCommit);

        CommitsRequest req = new CommitsRequest();
        req.setCommits(testCommitList);

        when(commitPersistence.findAllById(eq(projectId), any())).thenThrow(InternalErrorException.class);

        try {
            defaultCommitService.getCommits(projectId, req);
        } catch (InternalErrorException e) {
            assertNotNull(e);
        }

        resetMocks();
    }


    @Test
    public void getCommits() {
        String projectId = "PROJECT_ID";

        CommitJson testCommit = new CommitJson();
        testCommit.setCommitId("TEST_COMMIT_ID");
        testCommit.setCreator("TEST_CREATOR");
        testCommit.setCreated("TEST_CREATED");
        testCommit.setDocId("TEST_DOC_ID");
        testCommit.setComment("TEST_COMMENT");

        List<CommitJson> testCommitList = new ArrayList();
        testCommitList.add(testCommit);

        CommitsRequest req = new CommitsRequest();
        req.setCommits(testCommitList);

        when(commitPersistence.findAllById(eq(projectId), any())).thenReturn(testCommitList);

        CommitsResponse response = defaultCommitService.getCommits(projectId, req);

        assertFalse(response.getCommits().isEmpty());

        resetMocks();
    }


    @Test
    public void getCommitsCommitsNotFound() {
        String projectId = "PROJECT_ID";

        List<CommitJson> testCommitList = new ArrayList();
        CommitsRequest req = new CommitsRequest();
        req.setCommits(testCommitList);

        when(commitPersistence.findAllById(eq(projectId), any())).thenReturn(new ArrayList<>());

        try {
            defaultCommitService.getCommits(projectId, req);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void isProjectNewCommitsIsEmpty() {
        String projectId = "PROJECT_ID";

        when(commitPersistence.findAllByProjectId(projectId)).thenReturn(new ArrayList<>());

        assertTrue(defaultCommitService.isProjectNew(projectId));

        resetMocks();
    }

    @Test
    public void isProjectNewCommitsIsNull() {
        String projectId = "PROJECT_ID";

        when(commitPersistence.findAllByProjectId(projectId)).thenReturn(null);

        assertTrue(defaultCommitService.isProjectNew(projectId));

        resetMocks();
    }

    @Test
    public void isProjectNew() {
        String projectId = "PROJECT_ID";

        CommitJson testCommit = new CommitJson();
        testCommit.setCommitId("TEST_COMMIT_ID");
        testCommit.setCreator("TEST_CREATOR");
        testCommit.setCreated("TEST_CREATED");
        testCommit.setDocId("TEST_DOC_ID");
        testCommit.setComment("TEST_COMMENT");

        List<CommitJson> testCommitList = new ArrayList();
        testCommitList.add(testCommit);

        when(commitPersistence.findAllByProjectId(projectId)).thenReturn(testCommitList);

        assertFalse(defaultCommitService.isProjectNew(projectId));

        resetMocks();
    }

    public void resetMocks() {
        reset(branchPersistence);
        reset(commitPersistence);
    }
}
