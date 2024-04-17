package org.openmbee.mms.localauth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true)
public class UserPasswordRulesConfig {
    @Value("${user.password.allow_self_set_when_blank:false}")
    private boolean allowSelfSetPasswordsWhenBlank;

    public boolean isAllowSelfSetPasswordsWhenBlank() {
        return allowSelfSetPasswordsWhenBlank;
    }
}
