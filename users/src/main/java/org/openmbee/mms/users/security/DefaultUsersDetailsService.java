package org.openmbee.mms.users.security;

import org.openmbee.mms.core.dao.GroupPersistence;
import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.core.dao.UserPersistence;
import org.openmbee.mms.core.exceptions.ForbiddenException;
import org.openmbee.mms.json.UserJson;
import org.openmbee.mms.users.objects.UserCreateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
public abstract class DefaultUsersDetailsService implements UsersDetailsService<UserCreateRequest> {

    public static final String TYPE = "local";

    private PasswordEncoder passwordEncoder;
    private UserPasswordRulesConfig userPasswordRulesConfig;

    protected UserPersistence userPersistence;
    protected UserGroupsPersistence userGroupsPersistence;

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
    public UsersDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserJson> user = userPersistence.findByUsername(username);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException(
                String.format("No user found with username '%s'.", username));
        }
        return new DefaultUsersDetails(user.get(), userGroupsPersistence.findGroupsAssignedToUser(username));
    }
    
    
    public UserJson register(UserCreateRequest req) {
        UserJson user = new UserJson();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setPassword(encodePassword(req.getPassword()));
        user.setEnabled(true);
        user.setAdmin(req.isAdmin());
        user.setType(DefaultUsersDetailsService.TYPE);
        return saveUser(user);
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
        saveUser(user);
    }

    public UserJson update(UserCreateRequest req, UserJson user) {
        if (req.getEmail() != null &&
            !user.getEmail().equals(req.getEmail())
        ) {
            user.setEmail(req.getEmail());
        }
        if (req.getFirstName() != null &&
            !user.getFirstName().equals(req.getFirstName())
        ) {
            user.setFirstName(req.getFirstName());
        }
        if (req.getLastName() != null &&
            !user.getLastName().equals(req.getLastName())
        ) {
            user.setLastName(req.getLastName());
        }
        if (req.isEnabled() != null && user.isEnabled() != req.isEnabled())

        if (req.getType() != null) {
            user.setType(req.getType());
        }
        return saveUser(user);
    }

    private String encodePassword(String password) {
        return (password != null && !password.isBlank()) ? passwordEncoder.encode(password) : null;
    }

    @Autowired
    public void setUserPersistence(UserPersistence userPersistence) {
        this.userPersistence = userPersistence;
    }

    public UserJson saveUser(UserJson user) {
        return userPersistence.save(user);
    }

    public Collection<UserJson> getUsers() {
        return userPersistence.findAll();
    }


}
