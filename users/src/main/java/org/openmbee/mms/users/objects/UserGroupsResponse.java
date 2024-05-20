package org.openmbee.mms.users.objects;


import java.util.Collection;

import org.openmbee.mms.json.UserJson;

import io.swagger.v3.oas.annotations.media.Schema;

public class UserGroupsResponse {

    @Schema(required = true)
    private String user;

    @Schema(nullable = true)
    private Collection<String> groups;

    public UserGroupsResponse(){}

    public UserGroupsResponse(UserJson user, Collection<String> groups){
        this.user = user.getName();
        this.groups = groups;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Collection<String> getGroups() {
        return groups;
    }

    public void setGroups(Collection<String> groups) {
        this.groups = groups;
    }
}
