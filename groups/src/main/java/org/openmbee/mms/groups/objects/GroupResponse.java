package org.openmbee.mms.groups.objects;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;

import org.openmbee.mms.json.GroupJson;

public class GroupResponse {

    @Schema(required = true)
    private String group;

    @Schema(nullable = true)
    private Collection<String> users;

    public GroupResponse(){}

    public GroupResponse(GroupJson group, Collection<String> users){
        this.group = group.getName();
        this.users = users;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Collection<String> getUsers() {
        return users;
    }

    public void setUsers(Collection<String> users) {
        this.users = users;
    }
}
