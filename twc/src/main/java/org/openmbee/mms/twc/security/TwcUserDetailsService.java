package org.openmbee.mms.twc.security;

import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.core.dao.UserPersistence;
import org.openmbee.mms.json.UserJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TwcUserDetailsService implements UserDetailsService {

    private UserPersistence userPersistence;
    private UserGroupsPersistence userGroupsPersistence;

    @Autowired
    public void setUserPersistence(UserPersistence userPersistence) {
        this.userPersistence = userPersistence;
    }

    @Autowired
    public void setUserGroupsPersistence(UserGroupsPersistence userGroupsPersistence) {
        this.userGroupsPersistence = userGroupsPersistence;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserJson> userOptional = userPersistence.findByUsername(username);

        UserJson user;
        if (userOptional.isEmpty()) {
            user = addUser(username);
        } else {
            user = userOptional.get();
        }
        return new TwcUserDetails(user, userGroupsPersistence.findGroupsAssignedToUser(username));
    }

    public UserJson addUser(String username) {
        UserJson user = new UserJson();
        user.setUsername(username);
        //TODO: fill in user details from TWC
        user.setEnabled(true);
        return userPersistence.save(user);
    }

}
