package org.openmbee.mms.localuser.config;

import org.openmbee.mms.localuser.security.UserCreateRequest;
import org.openmbee.mms.localuser.security.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthProviderConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuthProviderConfig.class);

    private UserDetailsServiceImpl userDetailsService;
    private PasswordEncoder passwordEncoder;

    @Value("${mms.admin.username}")
    private String adminUsername;
    @Value("${mms.admin.password}")
    private String adminPassword;

    @Autowired
    public void setUserDetailsService(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        try {
            userDetailsService.loadUserByUsername(adminUsername);
        } catch (UsernameNotFoundException e) {
            UserCreateRequest req = new UserCreateRequest();
            req.setAdmin(true);
            req.setPassword(adminPassword);
            req.setUsername(adminUsername);
            userDetailsService.register(req);
            logger.info(String.format("Creating root user: %s with specified password.",
                adminUsername));
        }
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}
