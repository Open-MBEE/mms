package org.openmbee.mms.core.dao;

import org.openmbee.mms.json.ProjectJson;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProjectPersistence {

    ProjectJson save(ProjectJson projectJson);

    ProjectJson update(ProjectJson projectJson);

    Optional<ProjectJson> findById(String projectId);

    List<ProjectJson> findAllById(Set<String> projectIds);

    List<ProjectJson> findAll();

    Collection<ProjectJson> findAllByOrgId(String orgId);

    void archiveById(String projectId);

    void deleteById(String projectId);

    boolean inheritsPermissions(String projectId);

    boolean hasPublicPermissions(String projectId);
}
