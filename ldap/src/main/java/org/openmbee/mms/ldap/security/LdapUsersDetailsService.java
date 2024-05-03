package org.openmbee.mms.ldap.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.naming.ldap.LdapName;

import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.dao.GroupPersistence;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.json.UserJson;
import org.openmbee.mms.ldap.config.LdapCondition;
import org.openmbee.mms.ldap.config.LdapSecurityConfig;
import org.openmbee.mms.localauth.security.LocalUsersDetailsService;
import org.openmbee.mms.users.security.DefaultUsersDetails;
import org.openmbee.mms.users.security.DefaultUsersDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.HardcodedFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.stereotype.Service;

@Service
@Conditional(LdapCondition.class)
public class LdapUsersDetailsService extends DefaultUsersDetailsService {

    private static Logger logger = LoggerFactory.getLogger(LdapUsersDetailsService.class);


    public static final String TYPE = "ldap";

    @Value("${ldap.provider.base:#{null}}")
    private String providerBase;

    @Value("${ldap.user.attributes.objectClass:user}")
    private String userAttributesUserClass;

    @Value("${ldap.user.attributes.objectCategory:person}")
    private String userAttributesUserCategory;

    @Value("${ldap.user.attributes.username:uid}")
    private String userAttributesUsername;

    @Value("${ldap.user.attributes.firstname:givenname}")
    private String userAttributesFirstName;

    @Value("${ldap.user.attributes.lastname:sn}")
    private String userAttributesLastName;

    @Value("${ldap.user.attributes.dn:entrydn}")
    private String userAttributesDn;

    @Value("${ldap.user.attributes.email:mail}")
    private String userAttributesEmail;

    @Value("${ldap.user.attributes.update:24}")
    private int userAttributesUpdate;

    @Value("${ldap.user.search.base:#{''}}")
    private String userSearchBase;

    @Value("${ldap.group.role.attribute:cn}")
    private String groupRoleAttribute;

    @Value("${ldap.group.search.base:#{''}}")
    private String groupSearchBase;

    @Value("${ldap.group.search.filter:(uniqueMember={0})}")
    private String groupSearchFilter;

    @Autowired
    private SpringSecurityLdapTemplate ldapTemplate;

    @Autowired
    private GroupPersistence groupPersistence;

    @Override
    public void changeUserPassword(String username, String password, boolean asAdmin) {
        Optional<UserJson> userOptional = userPersistence.findByUsername(username);
        if(userOptional.isEmpty()) {
            throw new UsernameNotFoundException(
                    String.format("No user found with username '%s'.", username));
        }

        UserJson user = userOptional.get();
        if (user.getType() == LocalUsersDetailsService.TYPE) {
            super.changeUserPassword(username, password, asAdmin);
        } else {
            throw new BadRequestException("Unable to change passwords for non-local users");
        }
    }

    public UserJson update(DirContextOperations userData, UserJson user) {

        if (user.getEmail() == null ||
            !user.getEmail().equals(userData.getStringAttribute(userAttributesEmail))
        ) {
            String email = userData.getStringAttribute(userAttributesEmail);
            user.setEmail(email);
        }
        if (user.getFirstName() == null ||
            !user.getFirstName().equals(userData.getStringAttribute(userAttributesFirstName))
        ) {
            user.setFirstName(userData.getStringAttribute(userAttributesFirstName));
        }
        if (user.getLastName() == null ||
            !user.getLastName().equals(userData.getStringAttribute(userAttributesLastName))
        ) {
            user.setLastName(userData.getStringAttribute(userAttributesLastName));
        }
        if (userData.getStringAttribute(userAttributesDn) != null ||
            !user.getLastName().equals(userData.getStringAttribute(userAttributesDn))
        ) {
             user.setDistingushedName(userData.getStringAttribute(userAttributesDn));
        }

        return user;
    }

    

    public UserJson register(DirContextOperations userData) {
        return saveUser(create(userData));
    }

    @Override
    public DefaultUsersDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserJson> user = userPersistence.findByUsername(username);
        UserJson userJson = new UserJson();
        

        if (user.isEmpty()) {
            userJson = saveUser(getUser(username));
        } else {
            userJson = user.get();
            if (user.get().getType().equals(LdapUsersDetailsService.TYPE) && userJson.getModified() != null && Instant.parse(userJson.getModified()).isBefore(Instant.now().minus(userAttributesUpdate, ChronoUnit.HOURS))) {
                userJson = saveUser(getUser(username));
            }
        }
        return new DefaultUsersDetails(userJson, userGroupsPersistence.findGroupsAssignedToUser(username));
    }


    private UserJson getUser(String username) {
        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectclass", userAttributesUserClass))
        .and(new EqualsFilter("objectcategory", userAttributesUserCategory))
        .and(new EqualsFilter(userAttributesUsername, username));
        List<UserJson> users = ldapTemplate.search(userSearchBase, filter.encode(), new UserAttributesMapper());
        if (users.isEmpty()) {
            throw new UsernameNotFoundException(
                    String.format("No user found with username '%s'.", username));
        }
        return users.get(0);
    }

    private UserJson create(DirContextOperations userData) {
        String username = userData.getStringAttribute(userAttributesUsername);
        UserJson user = new UserJson();
        user.setUsername(username);
        user.setEnabled(true);
        user.setAdmin(false);
        user.setType(LdapUsersDetailsService.TYPE);
        user.setPassword(null);
        return update(userData, user);
    }

    public Collection<? extends GrantedAuthority> getUserAuthorties(String username) {
        Collection<GroupJson> definedGroups = groupPersistence.findAll();
        OrFilter orFilter = new OrFilter();
        UserJson user = getUser(username);
        for (GroupJson definedGroup : definedGroups) {
            orFilter.or(new EqualsFilter(groupRoleAttribute, definedGroup.getName()));
        }
        String userDn = user.getDistingushedName();
        AndFilter andFilter = new AndFilter();
                HardcodedFilter groupsFilter = new HardcodedFilter(
                    groupSearchFilter.replace("{0}", LdapEncoder.filterEncode(userDn)));
        andFilter.and(groupsFilter);
        andFilter.and(orFilter);
        String filter = andFilter.encode();
        Set<String> memberGroups = ldapTemplate
            .searchForSingleAttributeValues(groupSearchBase, filter, new Object[]{""}, groupRoleAttribute);
        logger.debug("LDAP search result: {}", Arrays.toString(memberGroups.toArray()));

        //Add groups to user

        Set<GroupJson> addGroups = new HashSet<>();
        
        for (String memberGroup : memberGroups) {
            Optional<GroupJson> group = groupPersistence.findByName(memberGroup);
            group.ifPresent(g -> userGroupsPersistence.addUserToGroup(g.getName(), user.getUsername()));
            group.ifPresent(addGroups::add);
        }

        if (logger.isDebugEnabled()) {
            if ((long) addGroups.size() > 0) {
                addGroups.forEach(group -> logger.debug("Group received: {}", group.getName()));
            } else {
                logger.debug("No configured groups returned from LDAP");
            }
        }


        List<GrantedAuthority> auths = AuthorityUtils
            .createAuthorityList(memberGroups.toArray(new String[0]));
        if (Boolean.TRUE.equals(user.isAdmin())) {
            auths.add(new SimpleGrantedAuthority(AuthorizationConstants.MMSADMIN));
        }
        auths.add(new SimpleGrantedAuthority(AuthorizationConstants.EVERYONE));
        return auths;
    }

    private class UserAttributesMapper implements ContextMapper<UserJson> {


        public UserJson mapFromContext(Object ctx) throws NamingException {
            DirContextAdapter context = (DirContextAdapter)ctx;
            if (context == null) {
                return null;
            }
            return create(context);
            // if (attributes.get("objectclass") != null) {
            //     user.setObjectclass(attributes.get("objectclass").get().toString());
            // }
            // if (attributes.get("distinguishedname") != null) {
            //     user.setDistinguishedname(attributes.get("distinguishedname").get().toString());
            // }
            // if (attributes.get("userPassword") != null) {
            //     user.setUserPassword(attributes.get("userPassword").get().toString());
            // }
            // if (attributes.get("cn") != null) {
            //     user.setCn(attributes.get("cn").get().toString());
            // }
            // if (attributes.get("telephoneNumber") != null) {
            //     user.setTelephoneNumber(attributes.get("telephoneNumber").get().toString());
            // }
            // if (attributes.get("lastlogoff") != null) {
            // // user.setLastlogoff(DateTimeFormat.forPattern("yyyy-MM-dd
            // // HH:mm:ss")
            // //
            // .parseDateTime(attributes.get("lastlogoff").get().toString()));
            // DateTimeFormatter formatter =
            // DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
            // DateTime dt =
            // formatter.parseDateTime(attributes.get("lastlogoff").get().toString());
            // user.setLastlogoff(new DateTime(
            //
            // dt
            //
            // ));
            // // }
            // if (attributes.get("userprincipalname") != null) {
            //     user.setUserprincipalname(attributes.get("userprincipalname").get().toString());
            // }
            // if (attributes.get("department") != null) {
            //     user.setDepartment(attributes.get("department").get().toString());
            // }
            // if (attributes.get("company") != null) {
            //     user.setCompany(attributes.get("company").get().toString());
            // }
            // if (attributes.get("mail") != null) {
            //     user.setMail(attributes.get("mail").get().toString());
            // }
            // if (attributes.get("streetAddress") != null) {
            //     user.setStreetAddress(attributes.get("streetAddress").get().toString());
            // }
            // if (attributes.get("st") != null) {
            //     user.setSt(attributes.get("st").get().toString());
            // }
            // if (attributes.get("postalCode") != null) {
            //     user.setPostalCode(attributes.get("postalCode").get().toString());
            // }
            // if (attributes.get("l") != null) {
            //     user.setL(attributes.get("l").get().toString());
            // }
            // if (attributes.get("description") != null) {
            //     user.setDescription(attributes.get("description").get().toString());
            // }
            // if (attributes.get("c") != null) {
            //     user.setC(attributes.get("c").get().toString());
            // }
            // if (attributes.get("countryCode") != null) {
            //     user.setCountryCode(attributes.get("countryCode").get().toString());
            // }
            // if (attributes.get("cn") != null) {
            //     user.setCn(attributes.get("cn").get().toString());
            // }
            // if (attributes.get("sn") != null) {
            //     user.setSn(attributes.get("sn").get().toString());
            // }
            // if (attributes.get("employeeID") != null) {
            //     user.setEmployeeId(attributes.get("employeeID").get().toString());
            // }
            // if (attributes.get("lastLogon") != null) {
            //     // user.setLastLogon(DateTimeFormat.forPattern("yyyy-MM-dd
            //     // HH:mm:ss")/*
            //     // .parseDateTime(attributes.get("lastLogon").get().toString()));*/

            //     DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
            //     DateTime dt = formatter.parseDateTime(attributes.get("lastLogon").get().toString());
            //     user.setLastLogon(new DateTime(

            //             dt

            //     ));
            // }
            // if (attributes.get("memberof") != null) {
            //     user.setMemberof(attributes.get("memberof").get().toString());
            // }
            // if (attributes.get("givenname") != null) {
            //     user.setGivenname(attributes.get("givenname").get().toString());
            // }
            // if (attributes.get("logoncount") != null) {
            //     user.setLogoncount(attributes.get("logoncount").get().toString());
            // }
            // if (attributes.get("displayName") != null) {
            //     user.setDisplayname(attributes.get("displayName").get().toString());
            // }
        }
    }

    
}
