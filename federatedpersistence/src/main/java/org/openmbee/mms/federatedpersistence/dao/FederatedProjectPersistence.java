package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.data.dao.OrgDAO;
import org.openmbee.mms.data.dao.ProjectDAO;
import org.openmbee.mms.data.dao.ProjectIndex;
import org.openmbee.mms.data.domains.global.Organization;
import org.openmbee.mms.data.domains.global.Project;
import org.openmbee.mms.json.ProjectJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Component("federatedProjectPersistence")
public class FederatedProjectPersistence implements ProjectPersistence {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private ProjectDAO projectDAO;
    private ProjectIndex projectIndexDAO;
    private OrgDAO orgRepository;

    @Autowired
    public FederatedProjectPersistence(ProjectDAO projectDAO, ProjectIndex projectIndexDAO, OrgDAO orgDAO) {
        this.projectDAO = projectDAO;
        this.projectIndexDAO = projectIndexDAO;
        this.orgRepository = orgDAO;
    }

    @Override
    public Optional<ProjectJson> findById(String projectId) {
        ContextHolder.setContext(projectId);
        Optional<Project> projectOption = projectDAO.findByProjectId(projectId);

        if (projectOption.isEmpty()) {
            return Optional.empty();
        }

        Project project = projectOption.get();

        Optional<ProjectJson> projectJsonOption = projectIndexDAO.findById(project.getDocId());
        if (projectJsonOption.isEmpty()) {
            logger.error("Federated data inconsistency: JSON Not found for {} with docId: {}",
                project.getProjectId(), project.getDocId());
            throw new NotFoundException("Project not found");
        }

        return projectJsonOption;
    }

    @Override
    public List<ProjectJson> findAllById(Set<String> projectIds) {
        return projectIds.stream().map(projectId -> {
            Optional<Project> projectOption = projectDAO.findByProjectId(projectId);
            if (projectOption.isPresent()) {
                Project project = projectOption.get();
                ContextHolder.setContext(project.getProjectId());
                ProjectJson projectJson = projectIndexDAO.findById(project.getDocId()).orElse(null);
                projectJson.setIsArchived(project.isDeleted());
                return projectJson;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<ProjectJson> findAll() {
        return projectDAO.findAll().stream().map(project -> {

            ContextHolder.setContext(project.getProjectId());
            ProjectJson projectJson = projectIndexDAO.findById(project.getDocId()).orElse(null);
            projectJson.setIsArchived(project.isDeleted());
            return projectJson;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Collection<ProjectJson> findAllByOrgId(String orgId) {
        Optional<Organization> org = orgRepository.findByOrganizationId(orgId);
        if (org.isEmpty()) {
            throw new NotFoundException("org not found");
        }
        if (org.get().getProjects() == null) {
            return List.of();
        }
        return org.get().getProjects().stream().map(project -> {
            ContextHolder.setContext(project.getProjectId());
            ProjectJson projectJson = projectIndexDAO.findById(project.getDocId()).orElse(null);
            projectJson.setIsArchived(project.isDeleted());
            return projectJson;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void deleteById(String projectId) {
        String message = "";
        try {
            ContextHolder.clearContext();
            projectDAO.delete(projectId);
        } catch (Exception e) {
            message.concat(e.getMessage());
        }
        try {
            ContextHolder.setContext(projectId);
            projectIndexDAO.delete(projectId);
        } catch (Exception e) {
            message.concat(e.getMessage());
        }
        if (!message.isEmpty()) {
            throw new InternalErrorException(message);
        }
    }

    @Override
    public boolean inheritsPermissions(String projectId) {
        Optional<Project> project = projectDAO.findByProjectId(projectId);
        if (project.isEmpty()) {
            throw new NotFoundException("project " + projectId + " not found");
        }
        return project.get().isInherit();
    }

    @Override
    public boolean hasPublicPermissions(String projectId) {
        Optional<Project> project = projectDAO.findByProjectId(projectId);
        if (project.isEmpty()) {
            throw new NotFoundException("project " + projectId + " not found");
        }
        return project.get().isPublic();
    }

    @Override
    public void archiveById(String projectId) {
        //TODO not called locally, otherwise delete
        ContextHolder.setContext(projectId);
        Optional<Project> project = this.projectDAO.findByProjectId(projectId);

        ContextHolder.setContext(projectId);
        Optional<ProjectJson> projectJsonOption = project.isPresent() ?
            projectIndexDAO.findById(project.get().getDocId()) : Optional.empty();
        if (project.isEmpty() || projectJsonOption.isEmpty()) {
            throw new NotFoundException("Project state is invalid, cannot delete.");
        }

        Project p = project.get();

        p.setDeleted(true);
        ContextHolder.setContext(null);
        projectDAO.save(p);

        ProjectJson projectJson = projectJsonOption.get();
        projectJson.setIsArchived(true);

        ContextHolder.setContext(projectId);
        projectIndexDAO.update(projectJson);
    }

    @Override
    public ProjectJson save(ProjectJson projectJson) {

        Optional<Organization> org = orgRepository.findByOrganizationId(projectJson.getOrgId());

        if(org.isEmpty()) {
            throw new NotFoundException("org not found");
        }

        if (projectJson.getDocId() == null || projectJson.getDocId().isEmpty()) {
            projectJson.setDocId(UUID.randomUUID().toString());
        }

        Project proj = new Project();
        proj.setProjectId(projectJson.getId());
        proj.setProjectName(projectJson.getName());
        proj.setOrganization(org.get());
        proj.setProjectType(projectJson.getProjectType());
        proj.setDocId(projectJson.getDocId());

        if (projectJson.isArchived() != null) {
            proj.setDeleted(projectJson.isArchived());
        } 

        try {
            projectDAO.save(proj);
            ContextHolder.setContext(projectJson.getProjectId());
            projectIndexDAO.create(projectJson);

            return projectJson;
        } catch (Exception e) {
            logger.error("Couldn't create project: {}", projectJson.getProjectId(), e);
            //Need to clean up in case of partial creation
            deleteById(projectJson.getProjectId());
            throw new InternalErrorException("Could not create project");
        }
    }

    @Override
    public ProjectJson update(ProjectJson projectJson) {

        ContextHolder.setContext(projectJson.getProjectId());

        Optional<Project> projOption = projectDAO.findByProjectId(projectJson.getProjectId());
        if (projOption.isPresent()) {
            Project proj = projOption.get();
            if (projectJson.getName() != null && !projectJson.getName().isEmpty()) {
                proj.setProjectName(projectJson.getName());
            }
            if (projectJson.getOrgId() != null && !projectJson.getOrgId().isEmpty()) {
                Optional<Organization> org = orgRepository.findByOrganizationId(projectJson.getOrgId());
                if (org.isPresent() && !org.get().getOrganizationId().isEmpty()) {
                    proj.setOrganization(org.get());
                } else {
                    throw new BadRequestException("Invalid organization");
                }
            }
            if (projectJson.isArchived() != null) {
                proj.setDeleted(projectJson.isArchived());
            }   
            projectJson.setDocId(proj.getDocId());
            
           
            projectDAO.save(proj);
            projectJson.setIsArchived(proj.isDeleted());
            ContextHolder.setContext(projectJson.getProjectId());
            return projectIndexDAO.update(projectJson);
        }
        throw new NotFoundException("Could not update project");
    }
}
