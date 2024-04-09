package org.openmbee.mms.federatedpersistence.permissions;

import org.openmbee.mms.core.builders.PermissionUpdatesResponseBuilder;
import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;

public class FederatedPermissionUpdatesResponseBuilder extends PermissionUpdatesResponseBuilder {

    private FederatedPermissionsUpdateResponseBuilder usersBuilder = new FederatedPermissionsUpdateResponseBuilder();
    private FederatedPermissionsUpdateResponseBuilder groupsBuilder = new FederatedPermissionsUpdateResponseBuilder();


    @Override
    public FederatedPermissionUpdatesResponseBuilder insertUsers(PermissionUpdateResponse permissionUpdateResponse) {
        usersBuilder.insert(permissionUpdateResponse);
        return this;
    }

    @Override
    public FederatedPermissionUpdatesResponseBuilder insertGroups(PermissionUpdateResponse permissionUpdateResponse) {
        groupsBuilder.insert(permissionUpdateResponse);
        return this;
    }

    @Override
    public PermissionUpdatesResponse getPermissionUpdatesReponse() {
        PermissionUpdatesResponse permissionUpdatesResponse = new PermissionUpdatesResponse();
        permissionUpdatesResponse.setInherit(this.inherit);
        permissionUpdatesResponse.setPublic(this.isPublic);
        permissionUpdatesResponse.setUsers(usersBuilder.getPermissionUpdateResponse());
        permissionUpdatesResponse.setGroups(groupsBuilder.getPermissionUpdateResponse());
        return permissionUpdatesResponse;
    }

    @Override
    public FederatedPermissionsUpdateResponseBuilder getUsers() {
        return usersBuilder;
    }

    @Override
    public FederatedPermissionsUpdateResponseBuilder getGroups() {
        return groupsBuilder;
    }

}
