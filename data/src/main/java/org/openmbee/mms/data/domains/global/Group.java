package org.openmbee.mms.data.domains.global;

import java.util.Collection;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name = "groups")
public class Group extends Base {

    public static final String NAME_COLUMN = "name";

    @Column(unique = true)
    private String name;

    @ManyToMany(mappedBy = "groups")
    private Collection<User> users;

    public Group() {}
    public Group(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<User> getUsers() {
        return users;
    }

    public void setUsers(Collection<User> users) {
        this.users = users;
    }
}
