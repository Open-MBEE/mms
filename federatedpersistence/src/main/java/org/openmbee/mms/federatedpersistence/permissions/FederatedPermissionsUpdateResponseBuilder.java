package org.openmbee.mms.federatedpersistence.permissions;

import org.openmbee.mms.core.builders.PermissionUpdateResponseBuilder;
import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.data.domains.global.*;

import java.util.Collection;

public class FederatedPermissionsUpdateResponseBuilder extends PermissionUpdateResponseBuilder {


    public void insertPermissionUpdates_OrgUserPerm(PermissionUpdateResponse.Action action, Collection<OrgUserPerm> perms) {
        perms.forEach(v -> insertPermissionUpdate(action, v));
    }

    public void insertPermissionUpdate(PermissionUpdateResponse.Action action, OrgUserPerm v) {
        if(v == null)
            return;

        PermissionUpdateResponse.PermissionUpdate update = new PermissionUpdateResponse.PermissionUpdate(
            action, v.getUser().getUsername(), v.getRole().getName(), v.getOrganization().getOrganizationId(), v.getOrganization().getOrganizationName(),
            null, null, null, false);
        doInsert(update);
    }

    public void insertPermissionUpdates_OrgGroupPerm(PermissionUpdateResponse.Action action, Collection<OrgGroupPerm> perms) {
        perms.forEach(v -> insertPermissionUpdate(action, v));
    }

    public void insertPermissionUpdate(PermissionUpdateResponse.Action action, OrgGroupPerm v) {
        if(v == null)
            return;

        PermissionUpdateResponse.PermissionUpdate update = new PermissionUpdateResponse.PermissionUpdate(
            action, v.getGroup().getName(), v.getRole().getName(), v.getOrganization().getOrganizationId(), v.getOrganization().getOrganizationName(),
            null, null, null, false);
        doInsert(update);
    }

    public void insertPermissionUpdates_ProjectUserPerm(PermissionUpdateResponse.Action action, Collection<ProjectUserPerm> perms) {
        perms.forEach(v -> insertPermissionUpdate(action, v));
    }

    public void insertPermissionUpdate(PermissionUpdateResponse.Action action, ProjectUserPerm v) {
        if(v == null)
            return;

        PermissionUpdateResponse.PermissionUpdate update = new PermissionUpdateResponse.PermissionUpdate(
            action, v.getUser().getUsername(), v.getRole().getName(), v.getProject().getOrganization().getOrganizationId(),
            v.getProject().getOrganization().getOrganizationName(), v.getProject().getProjectId(), v.getProject().getProjectName(),
            null, v.isInherited());
        doInsert(update);
    }

    public void insertPermissionUpdates_ProjectGroupPerm(PermissionUpdateResponse.Action action, Collection<ProjectGroupPerm> perms) {
        perms.forEach(v -> insertPermissionUpdate(action, v));
    }

    public void insertPermissionUpdate(PermissionUpdateResponse.Action action, ProjectGroupPerm v) {
        if(v == null)
            return;

        PermissionUpdateResponse.PermissionUpdate update = new PermissionUpdateResponse.PermissionUpdate(
            action, v.getGroup().getName(), v.getRole().getName(), v.getProject().getOrganization().getOrganizationId(),
            v.getProject().getOrganization().getOrganizationName(), v.getProject().getProjectId(), v.getProject().getProjectName(),
            null, v.isInherited());
        doInsert(update);
    }

    public void insertPermissionUpdates_BranchUserPerm(PermissionUpdateResponse.Action action, Collection<BranchUserPerm> perms) {
        perms.forEach(v -> insertPermissionUpdate(action, v));
    }

    public void insertPermissionUpdate(PermissionUpdateResponse.Action action, BranchUserPerm v) {
        if(v == null)
            return;

        PermissionUpdateResponse.PermissionUpdate update = new PermissionUpdateResponse.PermissionUpdate(
            action, v.getUser().getUsername(), v.getRole().getName(), v.getBranch().getProject().getOrganization().getOrganizationId(),
            v.getBranch().getProject().getOrganization().getOrganizationName(), v.getBranch().getProject().getProjectId(),
            v.getBranch().getProject().getProjectName(), v.getBranch().getBranchId(), v.isInherited());
        doInsert(update);
    }

    public void insertPermissionUpdates_BranchGroupPerm(PermissionUpdateResponse.Action action, Collection<BranchGroupPerm> perms) {
        perms.forEach(v -> insertPermissionUpdate(action, v));
    }

    public void insertPermissionUpdate(PermissionUpdateResponse.Action action, BranchGroupPerm v) {
        if(v == null)
            return;

        PermissionUpdateResponse.PermissionUpdate update = new PermissionUpdateResponse.PermissionUpdate(
            action, v.getGroup().getName(), v.getRole().getName(), v.getBranch().getProject().getOrganization().getOrganizationId(),
            v.getBranch().getProject().getOrganization().getOrganizationName(), v.getBranch().getProject().getProjectId(),
            v.getBranch().getProject().getProjectName(),v.getBranch().getBranchId(), v.isInherited());
        doInsert(update);
    }
}
