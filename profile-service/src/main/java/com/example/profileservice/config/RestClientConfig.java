package com.example.profileservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for RestClient.
 * <p>
 * Explicitly defines the {@link RestClient.Builder} bean. This is required to inject
 * the builder into {@link com.example.profileservice.service.KeycloakAdminClient}
 * and other services that rely on RestClient for HTTP requests.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
