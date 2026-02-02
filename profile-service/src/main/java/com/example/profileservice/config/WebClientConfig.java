package com.example.profileservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient.
 * <p>
 * Explicitly defines the {@link WebClient.Builder} bean. This is required to inject
 * the builder into {@link com.example.profileservice.service.KeycloakAdminClient}
 * and other services that rely on WebClient for HTTP requests.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
