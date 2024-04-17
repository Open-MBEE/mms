package org.openmbee.mms.data.domains.global;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import javax.persistence.*;

@Entity
@Table(name = "groups", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Group extends Base {

    public static final String NAME_COLUMN = "name";

    @Column(unique = true)
    private String name;

    @ManyToMany(mappedBy = "groups")
    private Collection<User> users;

    @JsonIgnore
    private VALID_GROUP_TYPES type;

    @JsonProperty("type")
    private String typeString;

    @JsonIgnore
    @OneToMany(mappedBy = "group", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Collection<GroupGroupPerm> groupPerms;

    @JsonIgnore
    @OneToMany(mappedBy = "group", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Collection<GroupUserPerm> userPerms;

    @JsonProperty("public")
    private boolean isPublic;

    public Group() {}
    public Group(String name) {
        this.name = name;
        this.type = VALID_GROUP_TYPES.REMOTE;
        this.typeString = VALID_GROUP_TYPES.REMOTE.toString().toLowerCase();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum VALID_GROUP_TYPES {LOCAL, REMOTE}

    public VALID_GROUP_TYPES getType() {
        return this.type;
    }

    public String getTypeString() {
        return this.typeString;
    }

    public void setType(VALID_GROUP_TYPES t) {
            this.type = t;
            this.typeString = t.toString().toLowerCase();
    }

    public void setType(String t) {
        if (t.equalsIgnoreCase("local")) {
            this.type = VALID_GROUP_TYPES.LOCAL;
        }else {
            this.type = VALID_GROUP_TYPES.REMOTE;
        }
        this.typeString = t;
    }

    public Collection<User> getUsers() {
        return users;
    }

    public void setUsers(Collection<User> users) {
        this.users = users;
    }

    public Collection<GroupGroupPerm> getGroupPerms() {
        return this.groupPerms;
    }

    public void setGroupPerms(Collection<GroupGroupPerm> groupPerms) {
        this.groupPerms = groupPerms;
    }

    public Collection<GroupUserPerm> getUserPerms() {
        return this.userPerms;
    }

    public void setUserPerms(Collection<GroupUserPerm> userPerms) {
        this.userPerms = userPerms;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }
}
