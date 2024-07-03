package org.openmbee.mms.permissions.objects;

import org.openmbee.mms.core.config.Privileges;

public class PermissionLookup {

    public enum Type {ORG, PROJECT, BRANCH, GROUP;}
    private Type type;
    private String orgId;
    private String projectId;
    private String refId;
    private String groupName;
    private Privileges privilege;
    private boolean allowAnonIfPublic;
    private boolean hasPrivilege;

    public boolean isHasPrivilege() {
        return hasPrivilege;
    }

    public void setHasPrivilege(boolean hasPrivilege) {
        this.hasPrivilege = hasPrivilege;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getGroupName() { return groupName; }

    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Privileges getPrivilege() {
        return privilege;
    }

    public void setPrivilege(Privileges privilege) {
        this.privilege = privilege;
    }

    public boolean isAllowAnonIfPublic() {
        return allowAnonIfPublic;
    }

    public void setAllowAnonIfPublic(boolean allowAnonIfPublic) {
        this.allowAnonIfPublic = allowAnonIfPublic;
    }
}
