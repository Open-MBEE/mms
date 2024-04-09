package org.openmbee.mms.twc.config;

import org.openmbee.mms.core.delegation.PermissionsDelegateFactory;
import org.openmbee.mms.twc.permissions.TwcPermissionsDelegateFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class TwcPermissionsConfig {

    @Bean
    @Order(-2)
    public PermissionsDelegateFactory getPermissionsDelegateFactory() {
        return new TwcPermissionsDelegateFactory();
    }
}
