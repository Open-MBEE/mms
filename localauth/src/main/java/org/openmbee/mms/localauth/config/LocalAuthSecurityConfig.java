package org.openmbee.mms.localauth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class LocalAuthSecurityConfig {

    private static Logger logger = LoggerFactory.getLogger(LocalAuthSecurityConfig.class);

    @Autowired
    public void configureDaoAuth(AuthenticationManagerBuilder auth,
            DaoAuthenticationProvider daoAuthenticationProvider) {
        auth.authenticationProvider(daoAuthenticationProvider);
    }
}
