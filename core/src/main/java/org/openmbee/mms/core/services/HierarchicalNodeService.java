package org.openmbee.mms.core.services;

import org.openmbee.mms.json.MountJson;
import org.springframework.data.util.Pair;

import java.util.List;

public interface HierarchicalNodeService extends NodeService {

    MountJson getProjectUsages(String projectId, String refId, String commitId, List<Pair<String, String>> saw, boolean restrictOnPermissions);
}
