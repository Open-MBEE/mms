package org.openmbee.mms.cameo.services;

import org.openmbee.mms.core.services.CommitService;
import org.openmbee.mms.crud.services.DefaultCommitService;
import org.openmbee.mms.json.CommitJson;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("cameoCommitService")
public class CameoCommitService extends DefaultCommitService implements CommitService {
    @Override
    public boolean isProjectNew(String projectId) {
        List<CommitJson> commits = commitPersistence.findByProjectAndRefAndTimestampAndLimit(projectId, "master", null, 2);
        return commits == null || commits.size() <= 1; // a cameo project gets 1 auto commit, so its still "new"
    }
}
