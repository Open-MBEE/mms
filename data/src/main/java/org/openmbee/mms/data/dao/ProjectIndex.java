package org.openmbee.mms.data.dao;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.openmbee.mms.json.ProjectJson;

public interface ProjectIndex {

    void create(String projectId);

    void create(ProjectJson project);

    Optional<ProjectJson> findById(String docId);

    List<ProjectJson> findAllById(Set<String> docIds);

    void delete(String projectId);

    ProjectJson update(ProjectJson projectJson);

}
