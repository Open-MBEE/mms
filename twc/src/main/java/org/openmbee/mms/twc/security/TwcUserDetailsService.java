package org.openmbee.mms.twc.security;

import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.core.dao.UserPersistence;
import org.openmbee.mms.json.UserJson;
import org.openmbee.mms.twc.config.TwcConfig;
import org.openmbee.mms.twc.exceptions.TwcConfigurationException;
import org.openmbee.mms.twc.utilities.AdminUtils;
import org.openmbee.mms.users.security.DefaultUsersDetailsService;
import org.openmbee.mms.users.security.DefaultUsersDetails;
import org.openmbee.mms.users.security.UsersDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TwcUserDetailsService extends DefaultUsersDetailsService {

    @Override
    public UsersDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserJson> userOptional = userPersistence.findByUsername(username);

        UserJson user;
        if (userOptional.isEmpty()) {
            user = addUser(username);
        } else {
            user = userOptional.get();
        }
        return new DefaultUsersDetails(user, userGroupsPersistence.findGroupsAssignedToUser(username));
    }

    public UserJson addUser(String username) {
        UserJson user = new UserJson();
        user.setUsername(username);
        user.setAdmin(false);
        //TODO: fill in user details from TWC
        user.setEnabled(true);

        return register(user);
    }

    public UserJson register(UserJson user) {
        return saveUser(user);
    }

    @Override
    public void changeUserPassword(String username, String password, boolean asAdmin) {
        throw new TwcConfigurationException(HttpStatus.BAD_REQUEST,
            "Cannot Modify Password. Users for this server are controlled by Teamwork Cloud");
    }

    public UserJson update(UserJson userData, UserJson saveUser) {
        if (saveUser.getEmail() == null ||
            !saveUser.getEmail().equals(userData.getEmail())
        ) {
            saveUser.setEmail(userData.getEmail());
        }
        if (saveUser.getFirstName() == null ||
            !saveUser.getFirstName().equals(userData.getFirstName())
        ) {
            saveUser.setFirstName(userData.getFirstName());
        }
        if (saveUser.getLastName() == null ||
            !saveUser.getLastName().equals(userData.getLastName())
        ) {
            saveUser.setLastName(userData.getLastName());
        }
        
        return saveUser(saveUser);
    }

    

}
