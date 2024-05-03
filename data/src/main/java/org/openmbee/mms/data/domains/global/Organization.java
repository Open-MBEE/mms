package org.openmbee.mms.data.domains.global;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organization extends Base {

    @JsonProperty("name")
    private String organizationName;

    @JsonProperty("id")
    @Column(unique = true)
    private String organizationId;

    @OneToMany(mappedBy = "organization")
    @JsonBackReference
    private Collection<Project> projects;

    @JsonIgnore
    @OneToMany(mappedBy = "organization", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Collection<OrgGroupPerm> groupPerms;

    @JsonIgnore
    @OneToMany(mappedBy = "organization", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Collection<OrgUserPerm> userPerms;

    @JsonProperty("public")
    private boolean isPublic;

    private boolean deleted;

    public Organization() {
        this.deleted = false;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public Collection<Project> getProjects() {
        return projects;
    }

    public void setProjects(Collection<Project> projects) {
        this.projects = projects;
    }

    public Collection<OrgGroupPerm> getGroupPerms() {
        return groupPerms;
    }

    public void setGroupPerms(Collection<OrgGroupPerm> groupPerms) {
        this.groupPerms = groupPerms;
    }

    public Collection<OrgUserPerm> getUserPerms() {
        return userPerms;
    }

    public void setUserPerms(Collection<OrgUserPerm> userPerms) {
        this.userPerms = userPerms;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
