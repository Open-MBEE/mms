package org.openmbee.mms.users.security;

import org.openmbee.mms.core.dao.GroupPersistence;
import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.core.dao.UserPersistence;
import org.openmbee.mms.json.UserJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
public abstract class AbstractUsersDetailsService<T extends Object> implements UsersDetailsService<T> {

    private UserPersistence userPersistence;
    private UserGroupsPersistence userGroupsPersistence;
    private GroupPersistence groupPersistence;


    public GroupPersistence getGroupPersistence() {
        return this.groupPersistence;
    }

    public UserPersistence getUserPersistence() {
        return this.userPersistence;
    }
    
    public UserGroupsPersistence getUserGroupsPersistence() {
        return this.userGroupsPersistence;
    }

    @Autowired
    public void setUserGroupsPersistence(UserGroupsPersistence userGroupsPersistence) {
        this.userGroupsPersistence = userGroupsPersistence;
    }

    @Override
    public UsersDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserJson> user = getUserPersistence().findByUsername(username);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException(
                String.format("No user found with username '%s'.", username));
        }
        return new DefaultUsersDetails(user.get(), userGroupsPersistence.findGroupsAssignedToUser(username));
    }

    @Autowired
    public void setUserPersistence(UserPersistence userPersistence) {
        this.userPersistence = userPersistence;
    }

    public UserJson saveUser(UserJson user) {
        return getUserPersistence().save(user);
    }

    public Collection<UserJson> getUsers() {
        return getUserPersistence().findAll();
    }


}
