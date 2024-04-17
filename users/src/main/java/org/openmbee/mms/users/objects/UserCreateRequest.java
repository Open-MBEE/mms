package org.openmbee.mms.users.objects;

import java.io.Serializable;

public class UserCreateRequest implements Serializable {

    private static final long serialVersionUID = -849270125785286560L;

    private String username;
    private String password;
    private String email;
    private String firstname;
    private String lastname;
    private boolean admin;
    private String type;
    private Boolean enabled;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstname;
    }

    public void setFirstName(String firstname) {
        this.firstname = firstname;
    }

    public String getLastName() {
        return lastname;
    }

    public void setLastName(String lastname) {
        this.lastname = lastname;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean isEnabled() { return enabled; }

    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
