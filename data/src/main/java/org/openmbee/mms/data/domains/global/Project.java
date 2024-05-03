package org.openmbee.mms.data.domains.global;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "projects")
public class Project extends Base {

    @JsonProperty("name")
    private String projectName;

    @JsonProperty("id")
    @Column(unique = true)
    private String projectId;

    @JsonProperty("projectType")
    private String projectType;

    private String docId;

    private String connectionString;

    @ManyToOne
    @JsonManagedReference
    private Organization organization;

    @JsonIgnore
    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Collection<Branch> branches;

    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Collection<Metadata> metadata;

    @JsonIgnore
    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Collection<ProjectGroupPerm> groupPerms;

    @JsonIgnore
    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Collection<ProjectUserPerm> userPerms;

    @JsonProperty("public")
    private boolean isPublic;

    private boolean inherit;

    private boolean deleted;

    public Project() {
    }

    public Project(String projectId, String projectName) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.deleted = false;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getOrgId() {
        return organization.getOrganizationId();
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public Collection<Metadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(Collection<Metadata> metadata) {
        this.metadata = metadata;
    }

    public Collection<Branch> getBranches() {
        return branches;
    }

    public void setBranches(Collection<Branch> branches) {
        this.branches = branches;
    }

    public Collection<ProjectGroupPerm> getGroupPerms() {
        return groupPerms;
    }

    public void setGroupPerms(Collection<ProjectGroupPerm> groupPerms) {
        this.groupPerms = groupPerms;
    }

    public Collection<ProjectUserPerm> getUserPerms() {
        return userPerms;
    }

    public void setUserPerms(Collection<ProjectUserPerm> userPerms) {
        this.userPerms = userPerms;
    }

    public boolean isInherit() {
        return inherit;
    }

    public void setInherit(boolean inherit) {
        this.inherit = inherit;
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
