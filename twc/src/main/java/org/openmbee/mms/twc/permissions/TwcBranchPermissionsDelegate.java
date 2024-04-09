package org.openmbee.mms.twc.permissions;

import org.openmbee.mms.core.delegation.PermissionsDelegate;
import org.openmbee.mms.core.objects.PermissionResponse;
import org.openmbee.mms.core.objects.PermissionUpdateRequest;
import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;
import org.openmbee.mms.json.RefJson;
import org.openmbee.mms.twc.TeamworkCloud;
import org.openmbee.mms.twc.exceptions.TwcConfigurationException;
import org.openmbee.mms.twc.utilities.TwcPermissionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Set;

public class TwcBranchPermissionsDelegate implements PermissionsDelegate{

	private RefJson branch;
	private TeamworkCloud teamworkCloud;
	private String workspaceId;
	private String resourceId;

	@Autowired
	private TwcPermissionUtils twcPermissionUtils;

	
	public TwcBranchPermissionsDelegate(RefJson branch, TeamworkCloud teamworkCloud, String workspaceId,
			String resourceId) {
		this.branch = branch;
		this.teamworkCloud = teamworkCloud;
		this.workspaceId = workspaceId;
		this.resourceId = resourceId;
	}

	@Override
	public boolean hasPermission(String user, Set<String> groups, String privilege) {
		//TODO:: update once branch permissions are implemented in TWC
		boolean hasPermission = false;

		if (teamworkCloud.hasTwcRoles(privilege)) {
			hasPermission = twcPermissionUtils.hasPermissionToAccessProject(workspaceId, resourceId, teamworkCloud,
					user, privilege, teamworkCloud.getTwcmmsRolesMap().get(privilege));
		}
		
		return hasPermission;
	}

    @Override
    public boolean hasGroupPermissions(String group, String privilege) {
        throw new TwcConfigurationException(HttpStatus.BAD_REQUEST,
            "Cannot Query Group Roles.  Permissions for this branch are controlled by Teamwork Cloud ("
                + teamworkCloud.getUrl() + ")");
    }

    @Override
    public void initializePermissions(String creator) {
        //Do nothing, permissions are already initialized in TWC
    }

    @Override
    public void initializePermissions(String creator, boolean inherit) {
        //Do nothing, permissions are already initialized in TWC
    }

    @Override
    public boolean setInherit(boolean isInherit) {
        //Do nothing, permission inheritance will be handled by TWC
        return false;
    }

    @Override
    public PermissionResponse getInherit() {
        return PermissionResponse.getDefaultResponse();
    }

    @Override
	public void setPublic(boolean isPublic) {
		throw new TwcConfigurationException(HttpStatus.BAD_REQUEST,
				"Cannot Modify Permissions.  Permissions for this branch are controlled by Teamwork Cloud ("
						+ teamworkCloud.getUrl() + ")");
	}

	@Override
	public PermissionUpdateResponse updateUserPermissions(PermissionUpdateRequest req) {
		throw new TwcConfigurationException(HttpStatus.BAD_REQUEST,
				"Cannot Update User Permissions.  Permissions for this branch are controlled by Teamwork Cloud ("
						+ teamworkCloud.getUrl() + ")");
	}

	@Override
	public PermissionUpdateResponse updateGroupPermissions(PermissionUpdateRequest req) {
		throw new TwcConfigurationException(HttpStatus.BAD_REQUEST,
				"Cannot Update Group Permissions.  Permissions for this branch are controlled by Teamwork Cloud ("
						+ teamworkCloud.getUrl() + ")");
	}

	@Override
	public PermissionResponse getUserRoles() {
		throw new TwcConfigurationException(HttpStatus.BAD_REQUEST,
				"Cannot Query User Roles.  Permissions for this branch are controlled by Teamwork Cloud ("
						+ teamworkCloud.getUrl() + ")");
	}

	@Override
	public PermissionResponse getGroupRoles() {
		throw new TwcConfigurationException(HttpStatus.BAD_REQUEST,
				"Cannot Query Group Roles.  Permissions for this branch are controlled by Teamwork Cloud ("
						+ teamworkCloud.getUrl() + ")");
	}

	@Override
	public PermissionUpdatesResponse recalculateInheritedPerms() {
		// Do nothing, permission inheritance will be handled by TWC
		return null;
	}

    public RefJson getBranch() {
        return branch;
    }

    public TeamworkCloud getTeamworkCloud() {
        return teamworkCloud;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

	public String getResourceId() {
		return resourceId;
	}
}
