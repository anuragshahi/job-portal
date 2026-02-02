package com.example.profileservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Service-specific security configuration.
 * <p>
 * The main SecurityFilterChain is auto-configured by common-security's
 * {@code ResourceServerSecurityAutoConfiguration}. Public endpoints are
 * configured via {@code security.resource-server.public-endpoints} property.
 * <p>
 * This class only provides service-specific beans like PasswordEncoder.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
