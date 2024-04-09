package org.openmbee.mms.core.delegation;

import org.openmbee.mms.core.objects.PermissionResponse;
import org.openmbee.mms.core.objects.PermissionUpdateRequest;
import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;

import java.util.Set;

public interface PermissionsDelegate {
    boolean hasPermission(String user, Set<String> groups, String privilege);
    boolean hasGroupPermissions(String group, String privilege);
    void initializePermissions(String creator);
    void initializePermissions(String creator, boolean inherit);
    boolean setInherit(boolean isInherit);
    PermissionResponse getInherit();
    void setPublic(boolean isPublic);
    PermissionUpdateResponse updateUserPermissions(PermissionUpdateRequest req);
    PermissionUpdateResponse updateGroupPermissions(PermissionUpdateRequest req);
    PermissionResponse getUserRoles();
    PermissionResponse getGroupRoles();
    PermissionUpdatesResponse recalculateInheritedPerms();
}
