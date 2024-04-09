package org.openmbee.mms.twc.maintenance;

import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.json.ProjectJson;
import org.openmbee.mms.twc.metadata.TwcMetadata;
import org.openmbee.mms.twc.metadata.TwcMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/adm/maintenance")
public class TWCMaintenanceController {

    private ProjectPersistence projectPersistence;
    private TwcMetadataService twcMetadataService;

    @Autowired
    public void setProjectPersistence(ProjectPersistence projectPersistence) {
        this.projectPersistence = projectPersistence;
    }

    @Autowired
    public void setTwcMetadataService(TwcMetadataService twcMetadataService){
        this.twcMetadataService = twcMetadataService;
    }

    @PreAuthorize(AuthorizationConstants.IS_MMSADMIN)
    @GetMapping(value = "/project/twcmetadata/{id}")
    @ResponseBody
    public TwcMetadata getProjectMetadata(@PathVariable String id){
        ProjectJson project = getProject(id);
        return twcMetadataService.getTwcMetadata(project);
    }


    @PreAuthorize(AuthorizationConstants.IS_MMSADMIN)
    @PostMapping(value = "/project/twcmetadata/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void updateProjectMetadata(@PathVariable String id, @RequestBody TwcMetadata twcMetadata){

        ProjectJson project = getProject(id);
        try{
            twcMetadataService.updateTwcMetadata(project, twcMetadata);
        }
        catch (Exception ex){
            throw new InternalErrorException(ex.getMessage());
        }
    }

    @PreAuthorize(AuthorizationConstants.IS_MMSADMIN)
    @DeleteMapping(value = "/project/twcmetadata/{id}")
    @ResponseBody
    public void deleteProjectMetadata(@PathVariable String id){
        ProjectJson project = getProject(id);
        twcMetadataService.deleteTwcMetadata(project);
    }

    private ProjectJson getProject(String projectId) {

        Optional<ProjectJson> project = projectPersistence.findById(projectId);

        return project.orElseGet(() -> {
            String notFound = "Project id: " + projectId + " not found";
            throw new NotFoundException(notFound);
        });

    }
}

