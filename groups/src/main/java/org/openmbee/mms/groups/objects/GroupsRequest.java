package org.openmbee.mms.groups.objects;

import io.swagger.v3.oas.annotations.media.Schema;
import org.openmbee.mms.json.GroupJson;

import java.util.List;

public class GroupsRequest {

    @Schema(required = true)
    private List<GroupJson> groups;

    public List<GroupJson> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupJson> groups) {
        this.groups = groups;
    }

}