package org.openmbee.mms.data.domains.global;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "group_group_perms",
    indexes = {
        @Index(columnList = "group_id"),
        @Index(columnList = "group_id,groupPerm_id")
    })
public class GroupGroupPerm extends Base {

    @ManyToOne
    private Group group;

    @ManyToOne
    private Group groupPerm;

    @ManyToOne
    private Role role;

    public GroupGroupPerm() {}

    public GroupGroupPerm(Group group, Group u, Role r) {
        this.group = group;
        this.groupPerm = u;
        this.role = r;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Group getGroupPerm() {
        return groupPerm;
    }

    public void setGroupPerm(Group group) {
        this.groupPerm = group;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
