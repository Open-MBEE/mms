package org.openmbee.mms.core.builders;

import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;

public class PermissionUpdatesResponseBuilder {

    protected Boolean inherit;
    protected Boolean isPublic;
    private PermissionUpdateResponseBuilder usersBuilder = new PermissionUpdateResponseBuilder();
    private PermissionUpdateResponseBuilder groupsBuilder = new PermissionUpdateResponseBuilder();

    public PermissionUpdatesResponseBuilder setInherit(Boolean inherit) {
        this.inherit = inherit;
        return this;
    }

    public PermissionUpdatesResponseBuilder setPublic(Boolean aPublic) {
        isPublic = aPublic;
        return this;
    }

    public PermissionUpdatesResponseBuilder insert(PermissionUpdatesResponse permissionUpdatesResponse) {
        this.inherit = or(this.inherit, permissionUpdatesResponse.getInherit());
        this.isPublic = or(this.isPublic, permissionUpdatesResponse.getPublic());
        this.insertUsers(permissionUpdatesResponse.getUsers());
        this.insertGroups(permissionUpdatesResponse.getGroups());
        return this;
    }

    public PermissionUpdatesResponseBuilder insertUsers(PermissionUpdateResponse permissionUpdateResponse) {
        usersBuilder.insert(permissionUpdateResponse);
        return this;
    }

    public PermissionUpdatesResponseBuilder insertGroups(PermissionUpdateResponse permissionUpdateResponse) {
        groupsBuilder.insert(permissionUpdateResponse);
        return this;
    }

    public PermissionUpdatesResponse getPermissionUpdatesReponse() {
        PermissionUpdatesResponse permissionUpdatesResponse = new PermissionUpdatesResponse();
        permissionUpdatesResponse.setInherit(this.inherit);
        permissionUpdatesResponse.setPublic(this.isPublic);
        permissionUpdatesResponse.setUsers(usersBuilder.getPermissionUpdateResponse());
        permissionUpdatesResponse.setGroups(groupsBuilder.getPermissionUpdateResponse());
        return permissionUpdatesResponse;
    }

    public PermissionUpdateResponseBuilder getUsers() {
        return usersBuilder;
    }

    public PermissionUpdateResponseBuilder getGroups() {
        return groupsBuilder;
    }

    private Boolean or(Boolean a, Boolean b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a || b;
    }
}
