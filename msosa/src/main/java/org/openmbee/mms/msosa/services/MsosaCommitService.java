package org.openmbee.mms.msosa.services;

import org.openmbee.mms.core.services.CommitService;
import org.openmbee.mms.crud.services.DefaultCommitService;
import org.openmbee.mms.json.CommitJson;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("msosaCommitService")
public class MsosaCommitService extends DefaultCommitService implements CommitService {
    @Override
    public boolean isProjectNew(String projectId) {
        List<CommitJson> commits = commitPersistence.findAllByProjectId(projectId);
        return commits == null || commits.size() <= 1; // a msosa project gets 1 auto commit, so its still "new"
    }
}
