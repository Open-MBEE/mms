package org.openmbee.mms.core.services;

import java.util.Set;
import org.openmbee.mms.core.objects.PermissionResponse;
import org.openmbee.mms.core.objects.PermissionUpdateRequest;
import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;

public interface PermissionService {

    void initOrgPerms(String orgId, String creator);

    void initProjectPerms(String projectId, boolean inherit, String creator);

    void initBranchPerms(String projectId, String branchId, boolean inherit, String creator);

    void initGroupPerms(String groupName, String creator);

    PermissionUpdatesResponse updateOrgUserPerms(PermissionUpdateRequest req, String orgId);

    PermissionUpdatesResponse updateOrgGroupPerms(PermissionUpdateRequest req, String orgId);

    PermissionUpdatesResponse updateProjectUserPerms(PermissionUpdateRequest req, String projectId);

    PermissionUpdatesResponse updateProjectGroupPerms(PermissionUpdateRequest req, String projectId);

    PermissionUpdateResponse updateBranchUserPerms(PermissionUpdateRequest req, String projectId, String branchId);

    PermissionUpdateResponse updateBranchGroupPerms(PermissionUpdateRequest req, String projectId, String branchId);

    PermissionUpdatesResponse updateGroupUserPerms(PermissionUpdateRequest req, String groupName);

    PermissionUpdatesResponse updateGroupGroupPerms(PermissionUpdateRequest req, String groupName);

    PermissionUpdatesResponse setProjectInherit(boolean isInherit, String projectId);

    PermissionUpdatesResponse setBranchInherit(boolean isInherit, String projectId, String branchId);

    boolean setOrgPublic(boolean isPublic, String orgId);

    boolean setProjectPublic(boolean isPublic, String projectId);

    boolean setGroupPublic(boolean isPublic, String groupName);

    boolean hasOrgPrivilege(String privilege, String user, Set<String> groups, String orgId);

    boolean hasProjectPrivilege(String privilege, String user, Set<String> groups, String projectId);

    boolean hasBranchPrivilege(String privilege, String user, Set<String> groups, String projectId, String branchId);

    boolean hasGroupPrivilege(String privilege, String user, Set<String> groups, String groupName);

    boolean isProjectInherit(String projectId);

    boolean isBranchInherit(String projectId, String branchId);

    boolean isOrgPublic(String orgId);

    boolean isProjectPublic(String projectId);

    boolean isGroupPublic(String groupName);

    PermissionResponse getOrgGroupRoles(String orgId);

    PermissionResponse getOrgUserRoles(String orgId);

    PermissionResponse getProjectGroupRoles(String projectId);

    PermissionResponse getProjectUserRoles(String projectId);

    PermissionResponse getBranchGroupRoles(String projectId, String branchId);

    PermissionResponse getBranchUserRoles(String projectId, String branchId);

    PermissionResponse getGroupGroupRoles(String groupName);

    PermissionResponse getGroupUserRoles(String groupName);
}
