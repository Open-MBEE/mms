package org.openmbee.mms.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({BaseJson.COMMITID, BaseJson.REFID, BaseJson.PROJECTID, BaseJson.TYPE, BaseJson.IS_DELETED,
    BaseJson.NAME, BaseJson.ID, UserJson.PASSWORD, "empty"})
@Schema(name = "User", requiredProperties = {UserJson.USERNAME})
public class UserJson extends BaseJson<UserJson> {

    public static final String USERNAME = "username";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String ADMIN = "admin";
    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";
    public static final String ENABLED = "enabled";

    public String getUsername() {
        return (String) get(USERNAME);
    }

    public UserJson setUsername(String username) {
        put(USERNAME, username);
        return this;
    }

    public String getFirstName() {
        return (String) get(FIRST_NAME);
    }

    public UserJson setFirstName(String firstName) {
        put(FIRST_NAME, firstName);
        return this;
    }

    public String getLastName() {
        return (String) get(LAST_NAME);
    }

    public UserJson setLastName(String lastName) {
        put(LAST_NAME, lastName);
        return this;
    }

    public Boolean isAdmin() {
        return (Boolean) get(ADMIN);
    }

    public UserJson setAdmin(Boolean admin) {
        put(ADMIN, admin);
        return this;
    }

    public String getEmail() {
        return (String) get(EMAIL);
    }

    public UserJson setEmail(String email) {
        put(EMAIL, email);
        return this;
    }

    public String getPassword() {
        return (String) get(PASSWORD);
    }

    public UserJson setPassword(String password) {
        put(PASSWORD, password);
        return this;
    }

    public Boolean isEnabled() {
        return (Boolean) get(ENABLED);
    }

    public UserJson setEnabled(Boolean enabled) {
        put(ENABLED, enabled);
        return this;
    }

}
