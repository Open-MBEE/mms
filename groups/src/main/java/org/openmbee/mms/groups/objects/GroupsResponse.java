package org.openmbee.mms.groups.objects;


import org.openmbee.mms.core.objects.BaseResponse;

import org.openmbee.mms.json.GroupJson;
import java.util.ArrayList;
import java.util.List;

public class GroupsResponse extends BaseResponse<GroupsResponse> {

    private List<GroupJson> groups;

    public GroupsResponse() {
        this.groups = new ArrayList<>();
    }

    public GroupsResponse(List<GroupJson> groups) {
        this.groups = groups;
    }

    public List<GroupJson> getGroups() {
        return this.groups;
    }

    public void setGroups(List<GroupJson> groups) {
        this.groups = groups;
    }

}
