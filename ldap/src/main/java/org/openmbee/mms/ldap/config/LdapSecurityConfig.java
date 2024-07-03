package org.openmbee.mms.ldap.config;

import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.dao.GroupPersistence;
import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.core.dao.UserPersistence;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.json.UserJson;
import org.openmbee.mms.ldap.security.LdapUsersDetailsService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.*;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.naming.Context;

@Configuration
@Conditional(LdapCondition.class)
@EnableTransactionManagement
public class LdapSecurityConfig {

    private static Logger logger = LoggerFactory.getLogger(LdapSecurityConfig.class);

    @Value("${ldap.ad.enabled:false}")
    private Boolean adEnabled;

    @Value("${ldap.ad.domain:#{null}}")
    private String adDomain;

    @Value("${ldap.provider.url:#{null}}")
    private String providerUrl;

    @Value("${ldap.provider.userdn:#{null}}")
    private String providerUserDn;

    @Value("${ldap.provider.password:#{null}}")
    private String providerPassword;

    @Value("${ldap.provider.base:#{null}}")
    private String providerBase;

    @Value("#{'${ldap.user.dn.pattern:uid={0}}'.split(';')}")
    private List<String> userDnPattern;

    @Value("${ldap.user.attributes.dn:entrydn}")
    private String userAttributesDn;

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

    @Value("${ldap.group.search.base:#{''}}")
    private String groupSearchBase;

    @Value("${ldap.group.role.attribute:cn}")
    private String groupRoleAttribute;

    @Value("${ldap.group.search.filter:(uniqueMember={0})}")
    private String groupSearchFilter;

    @Value("${ldap.user.search.base:#{''}}")
    private String userSearchBase;

    @Value("${ldap.user.search.filter:(uid={0})}")
    private String userSearchFilter;
    private UserPersistence userPersistence;
    private GroupPersistence groupPersistence;
    private UserGroupsPersistence userGroupsPersistence;
    private LdapUsersDetailsService ldapUsersDetailsService;
    
    @Autowired
    public void setUserPersistence(UserPersistence userPersistence) {
        this.userPersistence = userPersistence;
    }

    @Autowired
    public void setGroupPersistence(GroupPersistence groupPersistence) {
        this.groupPersistence = groupPersistence;
    }

    @Autowired
    public void setUserGroupsPersistence(UserGroupsPersistence userGroupsPersistence) {
        this.userGroupsPersistence = userGroupsPersistence;
    }

    @Autowired
    public void setLdapUsersDetailsService(LdapUsersDetailsService ldapUsersDetailsService) {
        this.ldapUsersDetailsService = ldapUsersDetailsService;
    }

    @Autowired
    public void configureLdapAuth(AuthenticationManagerBuilder auth,
        LdapAuthoritiesPopulator ldapAuthoritiesPopulator, @Qualifier("contextSource") BaseLdapPathContextSource contextSource)
        throws Exception {
        if (providerUrl != null) {
            logger.info("LDAP Module is loading...");
        /*
            see this article : https://spring.io/guides/gs/authenticating-ldap/
            We  redefine our own LdapAuthoritiesPopulator which need ContextSource().
            We need to delegate the creation of the contextSource out of the builder-configuration.
        */
            if (adEnabled) {
                auth.authenticationProvider(activeDirectoryLdapAuthenticationProvider());
            } else {
                String[] userPatterns = userDnPattern.toArray(new String[0]);
                LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> authProviderConfigurer = auth.ldapAuthentication();
                authProviderConfigurer.userDnPatterns(userPatterns);
                authProviderConfigurer.userSearchBase(userSearchBase);
                authProviderConfigurer.userSearchFilter(userSearchFilter);
                authProviderConfigurer.groupSearchBase(groupSearchBase);
                authProviderConfigurer.groupRoleAttribute(groupRoleAttribute);
                authProviderConfigurer.groupSearchFilter(groupSearchFilter);
                authProviderConfigurer.rolePrefix("");
                authProviderConfigurer.ldapAuthoritiesPopulator(ldapAuthoritiesPopulator);
                authProviderConfigurer.contextSource(contextSource);
            }
        }
    }

    @Bean
    LdapAuthoritiesPopulator ldapAuthoritiesPopulator() {

        /*
          Specificity here : we don't get the Role by reading the members of available groups (which is implemented by
          default in Spring security LDAP), but we retrieve the groups the user belongs to.
         */
        class CustomLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {

            private CustomLdapAuthoritiesPopulator() {}

            @Override
            public Collection<? extends GrantedAuthority> getGrantedAuthorities(
                DirContextOperations userData, String username) {
                logger.debug("Populating authorities using LDAP");
                return ldapUsersDetailsService.getUserAuthorities(username);
            }
        }

        return new CustomLdapAuthoritiesPopulator();

    }

    @Bean
    public AuthenticationProvider activeDirectoryLdapAuthenticationProvider() {

        class CustomActiveDirectoryLdapAuthenticationProvider implements AuthenticationProvider {

            private final ActiveDirectoryLdapAuthenticationProvider provider;

            public CustomActiveDirectoryLdapAuthenticationProvider(ActiveDirectoryLdapAuthenticationProvider provider) {
                this.provider = provider;

            }
            
            @Override
            public Authentication authenticate(Authentication authentication) {
                Authentication auth = provider.authenticate(authentication);
                return UsernamePasswordAuthenticationToken.authenticated(auth.getPrincipal(), auth.getCredentials(), ldapUsersDetailsService.getUserAuthorities(((UserDetails) auth.getPrincipal()).getUsername()));
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return provider.supports(authentication);
            }

        }
        ActiveDirectoryLdapAuthenticationProvider provider = new ActiveDirectoryLdapAuthenticationProvider(adDomain, providerUrl, providerBase);

        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, providerUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, providerUserDn);
        env.put(Context.SECURITY_CREDENTIALS, providerPassword);

        provider.setContextEnvironmentProperties(env);

        provider.setSearchFilter(userSearchFilter);
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setUseAuthenticationRequestCredentials(true);
        return new CustomActiveDirectoryLdapAuthenticationProvider(provider);
    }

    @Bean
    public LdapContextSource contextSource() {
        LdapContextSource contextSource = new LdapContextSource();

        logger.debug("Initializing LDAP ContextSource with the following values: ");

        contextSource.setUrl(providerUrl);
        contextSource.setBase(providerBase);
        contextSource.setUserDn(providerUserDn);
        contextSource.setPassword(providerPassword);

        logger.debug("BaseLdapPath: " + contextSource.getBaseLdapPathAsString());
        logger.debug("UserDn: " + contextSource.getUserDn());

        return contextSource;
    }

    @Bean
    public SpringSecurityLdapTemplate ldapTemplate() {
        return new SpringSecurityLdapTemplate(contextSource());
    }

   
}
