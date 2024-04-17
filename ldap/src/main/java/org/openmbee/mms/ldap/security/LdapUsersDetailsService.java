package org.openmbee.mms.ldap.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.openmbee.mms.core.exceptions.ForbiddenException;
import org.openmbee.mms.json.UserJson;
import org.openmbee.mms.users.security.AbstractUsersDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.stereotype.Service;

@Service
public class LdapUsersDetailsService extends AbstractUsersDetailsService<DirContextOperations> {

    private static Logger logger = LoggerFactory.getLogger(LdapUsersDetailsService.class);

    @Value("${ldap.provider.base:#{null}}")
    private String providerBase;

    @Value("${ldap.user.search.filter:(uid={0})}")
    private String userSearchFilter;

    @Value("${ldap.user.attributes.username:uid}")
    private String userAttributesUsername;

    @Value("${ldap.user.attributes.firstname:givenname}")
    private String userAttributesFirstName;

    @Value("${ldap.user.attributes.lastname:sn}")
    private String userAttributesLastName;

    @Value("${ldap.user.attributes.email:mail}")
    private String userAttributesEmail;

    @Value("${ldap.user.attributes.update:24}")
    private int userAttributesUpdate;

    @Value("${ldap.group.role.attribute:cn}")
    private String groupRoleAttribute;

    @Value("${ldap.group.search.filter:(uniqueMember={0})}")
    private String groupSearchFilter;

    public UserJson register(DirContextOperations userData) {
        String username = userData.getStringAttribute(userAttributesUsername);
        logger.debug("Creating user for {} using LDAP", username);
        UserJson user = update(userData, new UserJson());
        user.setUsername(username);
        user.setEnabled(true);
        user.setAdmin(false);
        return saveUser(user);
    }

    public void changeUserPassword(String username, String password, boolean asAdmin) {
            throw new ForbiddenException("Cannot change or set passwords for external users.");
    }

    public UserJson update(DirContextOperations userData, UserJson saveUser) {
        if (saveUser.getModified() != null && Instant.parse(saveUser.getModified()).isBefore(Instant.now().minus(userAttributesUpdate, ChronoUnit.HOURS))) {
            if (saveUser.getEmail() == null ||
                !saveUser.getEmail().equals(userData.getStringAttribute(userAttributesEmail))
            ) {
                saveUser.setEmail(userData.getStringAttribute(userAttributesEmail));
            }
            if (saveUser.getFirstName() == null ||
                !saveUser.getFirstName().equals(userData.getStringAttribute(userAttributesFirstName))
            ) {
                saveUser.setFirstName(userData.getStringAttribute(userAttributesFirstName));
            }
            if (saveUser.getLastName() == null ||
                !saveUser.getLastName().equals(userData.getStringAttribute(userAttributesLastName))
            ) {
                saveUser.setLastName(userData.getStringAttribute(userAttributesLastName));
            }

        }
                      
        saveUser.setPassword(null); 

        return saveUser(saveUser);
    }
    
}
