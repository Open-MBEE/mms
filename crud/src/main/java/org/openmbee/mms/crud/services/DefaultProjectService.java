package org.openmbee.mms.crud.services;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.dao.BranchPersistence;
import org.openmbee.mms.core.dao.OrgPersistence;
import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.objects.EventObject;
import org.openmbee.mms.core.objects.ProjectsResponse;
import org.openmbee.mms.core.services.EventService;
import org.openmbee.mms.core.services.ProjectService;
import org.openmbee.mms.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service("defaultProjectService")
@Primary
public class DefaultProjectService implements ProjectService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected ProjectPersistence projectPersistence;
    protected OrgPersistence orgPersistence;
    protected BranchPersistence branchPersistence;
    protected Collection<EventService> eventPublisher;

    @Autowired
    public void setProjectPersistence(ProjectPersistence projectPersistence) {
        this.projectPersistence = projectPersistence;
    }

    @Autowired
    public void setOrgPersistence(OrgPersistence orgDAO) {
        this.orgPersistence = orgDAO;
    }

    @Autowired
    public void setBranchPersistence(BranchPersistence branchPersistence) {
        this.branchPersistence = branchPersistence;
    }

    @Autowired
    public void setEventPublisher(Collection<EventService> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public ProjectJson create(ProjectJson project) {
        if (project.getOrgId() == null || project.getOrgId().isEmpty()) {
            throw new BadRequestException("Organization ID not provided");
        }

        Optional<OrgJson> org = orgPersistence.findById(project.getOrgId());
        if (org.isEmpty() || org.get().getId().isEmpty()) {
            throw new BadRequestException("Organization not found");
        }

        try {
            //TODO Transaction start
            ProjectJson savedProjectJson = projectPersistence.save(project);

            //create and save master branch. We're combining operations with the branch unifiedDAO.
            branchPersistence.save(createMasterRefJson(savedProjectJson));
            //TODO Transaction commit

            eventPublisher.forEach(pub -> pub.publish(
                EventObject.create(savedProjectJson.getId(), Constants.MASTER_BRANCH, "project_created", savedProjectJson)));
            return savedProjectJson;
        } catch (Exception e) {
            logger.error("Couldn't create project: {}", project.getProjectId(), e);
            //Need to clean up in case of partial creation
            projectPersistence.hardDelete(project.getProjectId());
            //TODO Transaction rollback (could include project delete in rollback)
        }
        throw new InternalErrorException("Could not create project");
    }

    public ProjectJson update(ProjectJson project) {
        if (project.getOrgId() != null && !project.getOrgId().isEmpty()) {
            Optional<OrgJson> org = orgPersistence.findById(project.getOrgId());
            if (org.isPresent() && !org.get().getId().isEmpty()) {
                project.setOrgId(org.get().getId());
            } else {
                throw new BadRequestException("Invalid organization");
            }
        }

        return projectPersistence.update(project);
    }

    public ProjectsResponse read(String projectId) {
        return null;
    }

    public boolean exists(String projectId) {
        Optional<ProjectJson> project = this.projectPersistence.findById(projectId);
        return project.isPresent() && project.get().getProjectId().equals(projectId);
    }


    public RefJson createMasterRefJson(ProjectJson project) {
        RefJson branchJson = new RefJson();
        branchJson.setId(Constants.MASTER_BRANCH);
        branchJson.setName(Constants.MASTER_BRANCH);
        branchJson.setParentRefId(null);
        branchJson.setRefType(RefType.Branch);
        branchJson.setCreated(project.getCreated());
        branchJson.setProjectId(project.getId());
        branchJson.setCreator(project.getCreator());
        branchJson.setDeleted(false);
        return branchJson;
    }
}
