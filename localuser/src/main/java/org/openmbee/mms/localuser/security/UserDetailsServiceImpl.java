package org.openmbee.mms.localuser.security;

import java.util.Collection;
import java.util.Optional;

import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.core.dao.UserPersistence;
import org.openmbee.mms.core.exceptions.ForbiddenException;
import org.openmbee.mms.json.UserJson;
import org.openmbee.mms.localuser.config.UserPasswordRulesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private UserPersistence userPersistence;
    private UserGroupsPersistence userGroupsPersistence;
    private PasswordEncoder passwordEncoder;
    private UserPasswordRulesConfig userPasswordRulesConfig;

    @Autowired
    public void setUserPersistence(UserPersistence userPersistence) {
        this.userPersistence = userPersistence;
    }

    @Autowired
    public void setUserGroupsPersistence(UserGroupsPersistence userGroupsPersistence) {
        this.userGroupsPersistence = userGroupsPersistence;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setUserPasswordRulesConfig(UserPasswordRulesConfig userPasswordRulesConfig) {
        this.userPasswordRulesConfig = userPasswordRulesConfig;
    }

    @Override
    public UserDetailsImpl loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserJson> user = userPersistence.findByUsername(username);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException(
                String.format("No user found with username '%s'.", username));
        }
        return new UserDetailsImpl(user.get(), userGroupsPersistence.findGroupsAssignedToUser(username));
    }

    public Collection<UserJson> getUsers() {
        return userPersistence.findAll();
    }

    public UserJson register(UserCreateRequest req) {
        UserJson user = new UserJson();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setFirstName(req.getFirstname());
        user.setLastName(req.getLastname());
        user.setPassword(encodePassword(req.getPassword()));
        user.setEnabled(true);
        user.setAdmin(req.isAdmin());
        return userPersistence.save(user);
    }

    public void changeUserPassword(String username, String password, boolean asAdmin) {
        Optional<UserJson> userOptional = userPersistence.findByUsername(username);
        if(userOptional.isEmpty()) {
            throw new UsernameNotFoundException(
                    String.format("No user found with username '%s'.", username));
        }

        UserJson user = userOptional.get();
        if(!asAdmin && !userPasswordRulesConfig.isAllowSelfSetPasswordsWhenBlank() &&
                (user.getPassword() == null || user.getPassword().isBlank())) {
            throw new ForbiddenException("Cannot change or set passwords for external users.");
        }

        //TODO password strength test?
        user.setPassword(encodePassword(password));
        userPersistence.save(user);
    }

    private String encodePassword(String password) {
        return (password != null && !password.isBlank()) ? passwordEncoder.encode(password) : null;
    }
}
