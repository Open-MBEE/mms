package org.openmbee.mms.data.domains.global;

import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "privileges")
public class Privilege extends Base {

    @Column(unique = true)
    private String name;

    @ManyToMany(mappedBy = "privileges", fetch = FetchType.EAGER)
    private Set<Role> roles;

    public Privilege() {
    }

    public Privilege(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
