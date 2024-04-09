package org.openmbee.mms.data.dao;

import java.util.List;
import java.util.Optional;
import org.openmbee.mms.data.domains.global.Project;

import javax.transaction.Transactional;

public interface ProjectDAO {

    Optional<Project> findByProjectId(String id);

    Optional<Project> findByProjectName(String name);

    List<Project> findAllByOrgId(String id);

    Project save(Project p);

    void delete(String projectId);

    List<Project> findAll();
}
