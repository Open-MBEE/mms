package org.openmbee.mms.core.dao;

import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.json.UserJson;

import java.util.Collection;

public interface UserGroupsPersistence {

    boolean addUserToGroup(String groupName, String username);
    boolean removeUserFromGroup(String groupName, String username);
    Collection<UserJson> findUsersInGroup(String groupName);
    Collection<GroupJson> findGroupsAssignedToUser(String username);

}
