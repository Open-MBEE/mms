package org.openmbee.mms.crud.services;

import java.util.Collection;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.core.dao.BranchDAO;
import org.openmbee.mms.core.dao.BranchIndexDAO;
import org.openmbee.mms.core.dao.OrgDAO;
import org.openmbee.mms.core.dao.ProjectDAO;
import org.openmbee.mms.core.dao.ProjectIndex;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.objects.EventObject;
import org.openmbee.mms.core.objects.ProjectsResponse;
import org.openmbee.mms.core.services.EventService;
import org.openmbee.mms.core.services.ProjectService;
import org.openmbee.mms.data.domains.global.Organization;
import org.openmbee.mms.data.domains.global.Project;
import org.openmbee.mms.data.domains.scoped.Branch;
import org.openmbee.mms.json.ProjectJson;
import org.openmbee.mms.json.RefJson;
import org.openmbee.mms.json.RefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service("defaultProjectService")
@Primary
public class DefaultProjectService implements ProjectService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected ProjectDAO projectRepository;
    protected OrgDAO orgRepository;
    protected ProjectIndex projectIndex;
    protected BranchDAO branchRepository;
    protected BranchIndexDAO branchIndex;
    protected Collection<EventService> eventPublisher;

    @Autowired
    public void setProjectRepository(ProjectDAO projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Autowired
    public void setOrganizationRepository(OrgDAO orgRepository) {
        this.orgRepository = orgRepository;
    }

    @Autowired
    public void setProjectIndex(ProjectIndex projectIndex) {
        this.projectIndex = projectIndex;
    }

    @Autowired
    public void setBranchRepository(BranchDAO branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Autowired
    public void setBranchIndex(BranchIndexDAO branchIndex) {
        this.branchIndex = branchIndex;
    }

    @Autowired
    public void setEventPublisher(Collection<EventService> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public ProjectJson create(ProjectJson project) {
        if (project.getOrgId() == null || project.getOrgId().isEmpty()) {
            throw new BadRequestException("Organization ID not provided");
        }

        Optional<Organization> org = orgRepository.findByOrganizationId(project.getOrgId());
        if (!org.isPresent() || org.get().getOrganizationId().isEmpty()) {
            throw new BadRequestException("Organization not found");
        }

        Project proj = new Project();
        proj.setProjectId(project.getId());
        proj.setProjectName(project.getName());
        proj.setOrganization(org.get());
        proj.setProjectType(project.getProjectType());

        String uuid = UUID.randomUUID().toString();
        proj.setDocId(uuid);
        project.setDocId(uuid);
        project.setCreated(Formats.FORMATTER.format(Instant.now()));
        project.setType("Project");

        try {
            projectRepository.save(proj);
            projectIndex.create(project);

            Optional<Branch> masterBranch = branchRepository.findByBranchId(Constants.MASTER_BRANCH);
            if (masterBranch.isPresent()) {
                String docId = UUID.randomUUID().toString();
                Branch master = masterBranch.get();
                master.setDocId(docId);
                master.setParentCommit(0L);

                branchRepository.save(master);

                RefJson branchJson = createRefJson(project, docId);
                branchIndex.index(branchJson);
            }
            eventPublisher.forEach((pub) -> pub.publish(
                EventObject.create(project.getId(), "master", "project_created", project)));
            return project;
        } catch (Exception e) {
            logger.error("Couldn't create project: {}", project.getProjectId(), e);
        }
        throw new InternalErrorException("Could not create project");
    }

    public RefJson createRefJson(ProjectJson project, String docId){
        RefJson branchJson = new RefJson();
        branchJson.setId(Constants.MASTER_BRANCH);
        branchJson.setName(Constants.MASTER_BRANCH);
        branchJson.setParentRefId(null);
        branchJson.setDocId(docId);
        branchJson.setRefType(RefType.Branch);
        branchJson.setCreated(project.getCreated());
        branchJson.setProjectId(project.getId());
        branchJson.setCreator(project.getCreator());
        branchJson.setDeleted(false);
        return branchJson;
    }

    public ProjectJson update(ProjectJson project) {
        Optional<Project> projOption = projectRepository.findByProjectId(project.getProjectId());
        if (projOption.isPresent()) {
            ContextHolder.setContext(project.getProjectId());
            Project proj = projOption.get();
            if (project.getName() != null && !project.getName().isEmpty()) {
                proj.setProjectName(project.getName());
            }
            if (project.getOrgId() != null && !project.getOrgId().isEmpty()) {
                Optional<Organization> org = orgRepository.findByOrganizationId(project.getOrgId());
                if (org.isPresent() && !org.get().getOrganizationId().isEmpty()) {
                    proj.setOrganization(org.get());
                } else {
                    throw new BadRequestException("Invalid organization");
                }
            }
            project.setDocId(proj.getDocId());
            projectRepository.save(proj);
            return projectIndex.update(project);
        }
        throw new InternalErrorException("Could not update project");
    }

    public ProjectsResponse read(String projectId) {
        return null;
    }

    public boolean exists(String projectId) {
        Optional<Project> project = this.projectRepository.findByProjectId(projectId);
        return project.isPresent() && project.get().getProjectId().equals(projectId);
    }
}
