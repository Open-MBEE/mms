package org.openmbee.mms.users.objects;

import java.util.Collection;
import org.openmbee.mms.json.UserJson;

public class UsersResponse {

    private Collection<UserJson> users;

    public Collection<UserJson> getUsers() {
        return users;
    }

    public void setUsers(Collection<UserJson> users) {
        this.users = users;
    }
}