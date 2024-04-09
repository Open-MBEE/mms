package org.openmbee.mms.core.services;

import org.openmbee.mms.core.objects.RefsResponse;
import org.openmbee.mms.json.RefJson;

public interface BranchService {

    RefsResponse getBranches(String projectId);

    RefsResponse getBranch(String projectId, String id);

    RefJson createBranch(String projectId, RefJson branch);

    RefJson updateBranch(String projectId, RefJson branch);

    RefsResponse deleteBranch(String projectId, String id);
}
