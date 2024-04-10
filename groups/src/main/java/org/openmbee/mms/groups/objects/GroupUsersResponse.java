package org.openmbee.mms.groups.objects;

import io.swagger.v3.oas.annotations.media.Schema;
import org.openmbee.mms.core.objects.BaseResponse;
import org.openmbee.mms.json.GroupJson;

import java.util.ArrayList;
import java.util.List;

public class GroupUsersResponse extends BaseResponse<GroupUsersResponse> {

    @Schema(required = true)
    private List<String> users;

    public GroupUsersResponse() {
        this.users = new ArrayList<>();
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }
}
