package org.openmbee.mms.rdb.repositories;

import java.util.List;
import java.util.Optional;
import org.openmbee.mms.data.domains.global.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByProjectId(String id);

    Optional<Project> findByProjectName(String name);

    List<Project> findAllByOrganizationOrganizationId(String id);

    List<Project> findAllByIsPublicTrue();
}
