package org.openmbee.mms.crud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.objects.ProjectsResponse;
import org.openmbee.mms.json.ProjectJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectDeleteService {

    private ProjectPersistence projectPersistence;
    protected ObjectMapper om;

    @Autowired
    public void setProjectPersistence(ProjectPersistence projectPersistence) {
        this.projectPersistence = projectPersistence;
    }

    @Autowired
    public void setOm(ObjectMapper om) {
        this.om = om;
    }

    public ProjectsResponse deleteProject(String projectId, boolean hard) {
        ProjectsResponse response = new ProjectsResponse();
        ProjectJson projectJson;
        ContextHolder.setContext(projectId);
        Optional<ProjectJson> projectJsonOption = projectPersistence.findById(projectId);

        List<ProjectJson> res = new ArrayList<>();

        //Do not try to do a soft delete when an error condition is present.
        if(projectJsonOption.isEmpty() && !hard) {
            throw new NotFoundException("Project state is invalid");
        }

        projectJson = projectJsonOption.orElseGet(() -> {
            ProjectJson newProject = new ProjectJson();
            newProject.setProjectId(projectId);
            return newProject;
        });

        if(hard){
            projectPersistence.hardDelete(projectId);
        } else {
            projectPersistence.softDelete(projectId);
        }

        projectJson.setIsDeleted(Constants.TRUE);
        res.add(projectJson);
        return response.setProjects(res);
    }
}
