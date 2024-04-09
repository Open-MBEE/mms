package org.openmbee.mms.core.builders;

import org.openmbee.mms.core.objects.PermissionUpdateResponse;

import java.util.*;

public class PermissionUpdateResponseBuilder {

    protected static class PermissionUpdateWrapper {

        private PermissionUpdateResponse.PermissionUpdate permissionUpdate;

        public PermissionUpdateWrapper(PermissionUpdateResponse.PermissionUpdate permissionUpdate) {
            this.permissionUpdate = permissionUpdate;
        }

        public PermissionUpdateResponse.PermissionUpdate getPermissionUpdate() {
            return permissionUpdate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PermissionUpdateWrapper that = (PermissionUpdateWrapper) o;
            return Objects.equals(getPermissionUpdate().getBranchId(), that.getPermissionUpdate().getBranchId())
                && Objects.equals(getPermissionUpdate().getProjectId(), that.getPermissionUpdate().getProjectId())
                && Objects.equals(getPermissionUpdate().getOrgId(), that.getPermissionUpdate().getOrgId())
                && Objects.equals(getPermissionUpdate().getName(), that.getPermissionUpdate().getName())
                && Objects.equals(getPermissionUpdate().isInherited(), that.getPermissionUpdate().isInherited());
        }
        @Override
        public int hashCode() {
            return Objects.hash(getPermissionUpdate().getBranchId(), getPermissionUpdate().getProjectId(),
                getPermissionUpdate().getOrgId(), getPermissionUpdate().getName(), getPermissionUpdate().isInherited());
        }
    }

    private HashSet<PermissionUpdateWrapper> removed = new HashSet<>();
    private HashSet<PermissionUpdateWrapper> added = new HashSet<>();

    public PermissionUpdateResponseBuilder insert(PermissionUpdateResponse updateUserPermissions) {
        updateUserPermissions.getPermissionUpdates().forEach(v -> doInsert(v));
        return this;
    }


    public PermissionUpdateResponse getPermissionUpdateResponse() {
        PermissionUpdateResponse response = new PermissionUpdateResponse();
        ArrayList<PermissionUpdateResponse.PermissionUpdate> updates = new ArrayList<>(removed.size() + added.size());
        removed.stream().map(PermissionUpdateWrapper::getPermissionUpdate).forEach(v -> updates.add(v));
        added.stream().map(PermissionUpdateWrapper::getPermissionUpdate).forEach(v -> updates.add(v));
        response.setPermissionUpdates(updates);
        return response;
    }

    protected void doInsert(PermissionUpdateResponse.PermissionUpdate update) {
        PermissionUpdateWrapper wrapped = new PermissionUpdateWrapper(update);
        if(update.getAction() == PermissionUpdateResponse.Action.ADD) {
            if(! removed.remove(wrapped)) {
                added.add(wrapped);
            }
        } else if(! added.remove(wrapped)){
            removed.add(wrapped);
        }

    }
}
