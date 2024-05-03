package org.openmbee.mms.users.security;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

public interface DefaultPasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder();
}