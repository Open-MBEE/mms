package org.openmbee.mms.rdb.repositories;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.data.dao.ProjectDAO;
import org.openmbee.mms.data.domains.global.Project;
import org.openmbee.mms.rdb.config.DatabaseDefinitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectDAOImpl implements ProjectDAO {

    private ProjectRepository projectRepository;

    private DatabaseDefinitionService projectOperations;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    public void setProjectRepository(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Autowired
    public void setDatabaseDefinitionService(DatabaseDefinitionService projectOperations) {
        this.projectOperations = projectOperations;
    }

    @Override
    public Optional<Project> findByProjectId(String id) {
        return projectRepository.findByProjectId(id);
    }

    @Override
    public Optional<Project> findByProjectName(String name) {
        return projectRepository.findByProjectName(name);
    }

    @Override
    public List<Project> findAllByOrgId(String id) {
        return projectRepository.findAllByOrganizationOrganizationId(id);
    }

    @Override
    public Project save(Project proj) {
        if (proj.getId() == null) {
            try {
                projectOperations.createProjectDatabase(proj);
            } catch (SQLException ex) {
                logger.error("Error creating project database. Database may already exist.", ex);
                throw new InternalErrorException(ex);
            }
        }
        return projectRepository.save(proj);
    }

    @Override
    public void delete(String projectId) {
        try {
            projectRepository.findByProjectId(projectId).ifPresent(v -> projectRepository.delete(v));
        } catch (Exception ex) {
            logger.error("Could not delete project from project Repository", ex);
            throw new InternalErrorException(ex);
        }

        try {
            projectOperations.deleteProjectDatabase(projectId);
        } catch (SQLException ex) {
            logger.error("DELETE PROJECT DATABASE EXCEPTION\nPotential connection issue, query statement mishap, or unexpected RDB behavior.", ex);
            throw new InternalErrorException(ex);
        }
    }

    @Override
    public List<Project> findAll() {
        return projectRepository.findAll();
    }
}
