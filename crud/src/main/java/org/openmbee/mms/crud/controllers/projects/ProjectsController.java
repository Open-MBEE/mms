package org.openmbee.mms.crud.controllers.projects;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openmbee.mms.core.config.*;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.DeletedException;
import org.openmbee.mms.core.exceptions.MMSException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.objects.ProjectsRequest;
import org.openmbee.mms.core.objects.ProjectsResponse;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.ProjectService;
import org.openmbee.mms.crud.CrudConstants;
import org.openmbee.mms.crud.controllers.BaseController;
import org.openmbee.mms.crud.services.ProjectDeleteService;
import org.openmbee.mms.json.ProjectJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;


@RestController
@RequestMapping("/projects")
@Tag(name = "Projects")
public class ProjectsController extends BaseController {

    private static final String PROJECT_ID_VALID_PATTERN = "^[\\w-]+$";

    private ProjectDeleteService projectDeleteService;
    private ProjectSchemas projectSchemas;

    @Autowired
    public void setProjectDeleteService(ProjectDeleteService projectDeleteService) {
        this.projectDeleteService = projectDeleteService;
    }

    @Autowired
    public void setProjectSchemas(ProjectSchemas projectSchemas) {
        this.projectSchemas = projectSchemas;
    }

    @GetMapping
    public ProjectsResponse getAllProjects(Authentication auth, @RequestParam(required = false) String orgId, @RequestParam(required = false, defaultValue = Constants.FALSE) boolean includeArchived) {

        ProjectsResponse response = new ProjectsResponse();
        Collection<ProjectJson> allProjects =
            orgId == null ? projectPersistence.findAll() : projectPersistence.findAllByOrgId(orgId);
        for (ProjectJson projectJson : allProjects) {
            try {
                if (mss.hasProjectPrivilege(auth, projectJson.getProjectId(), Privileges.PROJECT_READ.name(), true)
                        && projectJson.getDocId() != null
                        && (!Constants.TRUE.equals(projectJson.getIsArchived()) || includeArchived)) {
                    response.getProjects().add(projectJson);
                }
            } catch(NotFoundException ex) {
                logger.error("Project {} was not found when getting all projects.", projectJson.getProjectId());
            }
        }
        return response;
    }

    @GetMapping(value = "/{projectId}")
    @PreAuthorize("@mss.hasProjectPrivilege(authentication, #projectId, 'PROJECT_READ', true)")
    public ProjectsResponse getProject(
        @PathVariable String projectId) {

        ContextHolder.setContext(projectId);
        ProjectsResponse response = new ProjectsResponse();
        Optional<ProjectJson> projectOption = projectPersistence.findById(projectId);
        if (projectOption.isEmpty()) {
            throw new NotFoundException(response.addMessage("Project not found"));
        }
        response.getProjects().add(projectOption.get());
        return response;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ProjectsResponse createOrUpdateProjects(
        @RequestBody ProjectsRequest projectsPost,
        Authentication auth) {

        if (projectsPost.getProjects().isEmpty()) {
            throw new BadRequestException(new ProjectsResponse().addMessage("No projects provided"));
        }

        ProjectsResponse response = new ProjectsResponse();
        for (ProjectJson json: projectsPost.getProjects()) {
            try {
                if (json.getProjectId() == null || json.getProjectId().isEmpty()) {
                    json.setId(UUID.randomUUID().toString());
                } else if (!isProjectIdValid(json.getProjectId())) {
                    response.addRejection(new Rejection(json, 400, "Project id is invalid."));
                    continue;
                }

                Optional<ProjectJson> existingOptional = projectPersistence.findById(json.getProjectId());
                if (existingOptional.isPresent()) {
                    //Project exists, should merge the json, but not if schema is different
                    if (json.getProjectType() != null && !json.getProjectType().equals(existingOptional.get().getProjectType())) {
                        response.addRejection(new Rejection(json, 400, "Cannot change existing project schema"));
                        continue;
                    }
                    json.merge(existingOptional.get());
                } else {
                    //New Project
                    if (json.getCreated() == null || json.getCreated().isEmpty()) {
                        json.setCreated(Formats.FORMATTER.format(Instant.now()));
                    }
                    if (json.getType() == null || json.getType().isEmpty()) {
                        json.setType(CrudConstants.PROJECT);
                    }
                    if ((json.getProjectType() != null) && (!((projectSchemas.getSchemas())
                            .containsKey(json.getProjectType())))) {
                        response.addRejection(new Rejection(json, 400, "Project schema is unknown."));
                        continue;
                    }
                    //This needs to be used for create and update
                    if (json.getCreator() == null || json.getCreator().isEmpty()) {
                        json.setCreator(auth.getName());
                    }
                }

                ProjectService ps = getProjectService(json);
                String projectId = json.getProjectId();
                if (!ps.exists(projectId)) {
                    if (!mss.hasOrgPrivilege(auth, json.getOrgId(),
                        Privileges.ORG_CREATE_PROJECT.name(), false)) {
                        response.addRejection(new Rejection(json, 403, "No permission to create project under org"));
                        continue;
                    }
                    response.getProjects().add(ps.create(json));
                    permissionService.initProjectPerms(projectId, true, auth.getName());
                } else {
                    if (!mss.hasProjectPrivilege(auth, json.getProjectId(),
                        Privileges.PROJECT_EDIT.name(), false)) {
                        response.addRejection(new Rejection(json, 403, "No permission to change project"));
                        continue;
                    }
                    boolean updatePermissions = false;
                    if (json.getOrgId() != null && !json.getOrgId().isEmpty()) {
                        Optional<ProjectJson> projectJsonOption = projectPersistence.findById(json.getProjectId());
                        if (projectJsonOption.isPresent()) {
                            ProjectJson projectJson = projectJsonOption.get();
                            String existingOrg = projectJson.getOrgId();
                            if (!json.getOrgId().equals(existingOrg)) {
                                if (!mss.hasProjectPrivilege(auth, json.getProjectId(), Privileges.PROJECT_DELETE.name(), false) ||
                                        !mss.hasOrgPrivilege(auth, json.getOrgId(), Privileges.ORG_CREATE_PROJECT.name(), false)) {
                                    response.addRejection(
                                        new Rejection(json, 403, "No permission to move project org"));
                                    continue;
                                }
                                if (projectPersistence.inheritsPermissions(projectJson.getProjectId())) {
                                    updatePermissions = true;
                                }
                            }
                        }
                    }
                    response.getProjects().add(ps.update(json));
                    if (updatePermissions) {
                        permissionService.setProjectInherit(false, json.getProjectId());
                        permissionService.setProjectInherit(true, json.getProjectId());
                    }
                }
            } catch (MMSException ex) {
                response.addRejection(new Rejection(json, ex.getCode().value(), ex.getMessageObject().toString()));
            }
        }
        if (projectsPost.getProjects().size() == 1) {
            handleSingleResponse(response);
        }
        return response;
    }


    @DeleteMapping(value = "/{projectId}")
    @PreAuthorize("@mss.hasProjectPrivilege(authentication, #projectId, 'PROJECT_DELETE', false)")
    public ProjectsResponse deleteProject(
        @PathVariable String projectId,
        @RequestParam(required = false, defaultValue = Constants.FALSE) boolean hard) {

        if(!isProjectIdValid(projectId)) {
            throw new BadRequestException("Invalid project id");
        }
        return projectDeleteService.deleteProject(projectId, hard);
    }

    private ProjectService getProjectService(ProjectJson json) {
        String type = json.getProjectType();
        if (type == null || type.isEmpty()) {
            try {
                type = this.getProjectType(json.getProjectId());
            } catch (NotFoundException e) {
                type = CrudConstants.DEFAULT;
            }
            json.setProjectType(type);
        }
        return serviceFactory.getProjectService(type);
    }

    static boolean isProjectIdValid(String projectId) {
        return projectId != null && projectId.matches(PROJECT_ID_VALID_PATTERN);
    }
}
