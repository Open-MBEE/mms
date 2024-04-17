package org.openmbee.mms.core.objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class PermissionUpdateResponse {

    public enum Action {ADD, REMOVE}

    private List<PermissionUpdate> permissionUpdates;

    public static class PermissionUpdate {
        private Action action;

        private String name;

        private String role;

        private String orgId;

        private String orgName;

        private String projectId;

        private String projectName;

        private String branchId;

        private boolean inherited;

        private String groupName;

        public PermissionUpdate() {}

        public PermissionUpdate(Action action, String name, String role, String orgId, String orgName,
                                String projectId, String projectName, String branchId, String groupName, boolean inherited) {
            this.action = action;
            this.name = name;
            this.role = role;
            this.orgId = orgId;
            this.orgName = orgName;
            this.projectId = projectId;
            this.projectName = projectName;
            this.branchId = branchId;
            this.inherited = inherited;
            this.groupName = groupName;
        }

        public Action getAction() {
            return action;
        }

        public void setAction(Action action) {
            this.action = action;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getOrgId() {
            return orgId;
        }

        public void setOrgId(String orgId) {
            this.orgId = orgId;
        }

        public String getOrgName() {
            return orgName;
        }

        public void setOrgName(String orgName) {
            this.orgName = orgName;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getBranchId() {
            return branchId;
        }

        public void setBranchId(String branchId) {
            this.branchId = branchId;
        }

        public boolean isInherited() {
            return inherited;
        }

        public void setInherited(boolean inherited) {
            this.inherited = inherited;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }
    }

    public List<PermissionUpdate> getPermissionUpdates() {
        return permissionUpdates;
    }

    public void setPermissionUpdates(List<PermissionUpdate> permissionUpdates) {
        this.permissionUpdates = permissionUpdates;
    }

}
