package org.openmbee.mms.users.security;

import org.openmbee.mms.json.UserJson;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;

public interface UsersDetailsService<T extends Object> extends org.springframework.security.core.userdetails.UserDetailsService {

    @Override
    UsersDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    UserJson register(T registerUserObject);

    UserJson saveUser(UserJson user);

    void changeUserPassword(String username, String password, boolean asAdmin);

    Collection<UserJson> getUsers();

    UserJson update(T updateUserObject, UserJson user);
}
