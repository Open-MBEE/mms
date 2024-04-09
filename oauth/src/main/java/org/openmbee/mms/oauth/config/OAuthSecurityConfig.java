package org.openmbee.mms.oauth.config;

import org.openmbee.mms.oauth.security.OAuthAuthenticationFilter;
import org.openmbee.mms.oauth.security.OAuthProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class OAuthSecurityConfig {

    private static Logger logger = LoggerFactory.getLogger(OAuthSecurityConfig.class);

    public OAuthSecurityConfig() {
    }

    public void setAuthConfig(HttpSecurity http) throws Exception {
    
    } 

    @Bean
    public OAuthAuthenticationFilter oAuthAuthenticationFilter() throws Exception {
        return new OAuthAuthenticationFilter();
    }

    @Bean
    public OAuthProcessor oAuthProcessor() throws Exception {
        return new OAuthProcessor();
    }
}
