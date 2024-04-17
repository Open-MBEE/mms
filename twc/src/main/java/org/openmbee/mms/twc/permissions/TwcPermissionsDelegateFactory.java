package org.openmbee.mms.twc.permissions;

import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.core.delegation.PermissionsDelegate;
import org.openmbee.mms.core.delegation.PermissionsDelegateFactory;
import org.openmbee.mms.json.OrgJson;
import org.openmbee.mms.json.ProjectJson;
import org.openmbee.mms.json.RefJson;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.twc.TeamworkCloud;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.twc.config.TwcConfig;
import org.openmbee.mms.twc.exceptions.TwcConfigurationException;
import org.openmbee.mms.twc.metadata.TwcMetadata;
import org.openmbee.mms.twc.metadata.TwcMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.util.Optional;

public class TwcPermissionsDelegateFactory implements PermissionsDelegateFactory {

    private ApplicationContext applicationContext;
    private TwcConfig twcConfig;
    private TwcMetadataService twcMetadataService;
    private ProjectPersistence projectPersistence;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Autowired
    public void setTwcConfig(TwcConfig twcConfig) {
        this.twcConfig = twcConfig;
    }

    @Autowired
    public void setTwcMetadataService(TwcMetadataService twcMetadataService) {
        this.twcMetadataService = twcMetadataService;
    }

    @Autowired
    public void setProjectPersistence(ProjectPersistence projectPersistence) {
        this.projectPersistence = projectPersistence;
    }

    @Override
    public PermissionsDelegate getPermissionsDelegate(ProjectJson project) {
        if(!twcConfig.isUseAuthDelegation()) {
            return null;
        }

        TwcProjectDetails twcProjectDetails = getTwcDetails(project);
        if(twcProjectDetails != null) {
            return autowire(new TwcProjectPermissionsDelegate(project, twcProjectDetails.getTeamworkCloud(),
                twcProjectDetails.getWorkspaceId(), twcProjectDetails.getResourceId()));
        }

        return null;
    }

    @Override
    public PermissionsDelegate getPermissionsDelegate(OrgJson organization) {
        if(!twcConfig.isUseAuthDelegation()) {
            return null;
        }

        //TODO implement this once category-level permissions are implemented in TWC
        return null;
    }

    @Override
    public PermissionsDelegate getPermissionsDelegate(RefJson branch) {
        if(!twcConfig.isUseAuthDelegation()) {
            return null;
        }

        Optional<ProjectJson> projectOptional = projectPersistence.findById(branch.getProjectId());

        if(projectOptional.isEmpty()) {
            throw new NotFoundException("project not found");
        }

        TwcProjectDetails twcProjectDetails = getTwcDetails(projectOptional.get());
        if(twcProjectDetails != null) {
            return autowire(new TwcBranchPermissionsDelegate(branch, twcProjectDetails.getTeamworkCloud(),
                twcProjectDetails.getWorkspaceId(), twcProjectDetails.getResourceId()));
        }
        return null;
    }

    @Override
    public PermissionsDelegate getPermissionsDelegate(GroupJson group) {
        if(!twcConfig.isUseAuthDelegation()) {
            return null;
        }

        //Do nothing group permissions are handled by TWC
        return null;
    }

    private PermissionsDelegate autowire(PermissionsDelegate permissionsDelegate) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(permissionsDelegate);
        return permissionsDelegate;
    }

    private class TwcProjectDetails {

        private TeamworkCloud teamworkCloud;
        private String workspaceId;
        private String resourceId;

        public TeamworkCloud getTeamworkCloud() {
            return teamworkCloud;
        }

        public void setTeamworkCloud(TeamworkCloud teamworkCloud) {
            this.teamworkCloud = teamworkCloud;
        }

        public String getWorkspaceId() {
            return workspaceId;
        }

        public void setWorkspaceId(String workspaceId) {
            this.workspaceId = workspaceId;
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }
    }

    private TwcProjectDetails getTwcDetails(ProjectJson project) {
        TwcMetadata twcMetadata = null;
        try {
            twcMetadata = twcMetadataService.getTwcMetadata(project);
        }
        catch (NotFoundException e){ }

        if(twcMetadata == null || ! twcMetadata.isComplete()) {
            return null;
        }

        TeamworkCloud teamworkCloud = twcConfig.getTeamworkCloud(twcMetadata.getHost());

        if(teamworkCloud == null) {
            throw new TwcConfigurationException(HttpStatus.FAILED_DEPENDENCY,
                "Project " + project.getProjectId() + " (" + project.getName()
                    + ") is associated with an untrusted TWC host (" + twcMetadata.getHost() + ")");
        }

        TwcProjectDetails twcProjectDetails = new TwcProjectDetails();
        twcProjectDetails.setTeamworkCloud(teamworkCloud);
        twcProjectDetails.setWorkspaceId(twcMetadata.getWorkspaceId());
        twcProjectDetails.setResourceId(twcMetadata.getResourceId());

        return twcProjectDetails;
    }
}
