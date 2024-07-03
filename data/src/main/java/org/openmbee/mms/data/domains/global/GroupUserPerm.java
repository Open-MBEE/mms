package org.openmbee.mms.data.domains.global;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "group_user_perms",
    indexes = {
        @Index(columnList = "group_id"),
        @Index(columnList = "group_id,user_id")
    })
public class GroupUserPerm extends Base {

    @ManyToOne
    private Group group;

    @ManyToOne
    private User user;

    @ManyToOne
    private Role role;

    public GroupUserPerm() {}

    public GroupUserPerm(Group p, User u, Role r) {
        this.group = p;
        this.user = u;
        this.role = r;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
