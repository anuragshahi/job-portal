package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Configuration for rate limiting key resolution.
 * <p>
 * Uses the authenticated user's ID (JWT subject) as the rate limit key.
 * Falls back to client IP for unauthenticated requests.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> "user:" + principal.getName())
                .switchIfEmpty(Mono.defer(() -> {
                    // Fallback to IP for unauthenticated requests
                    String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
                    String ip;
                    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                        ip = xForwardedFor.split(",")[0].trim();
                    } else {
                        ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                                .getAddress().getHostAddress();
                    }
                    return Mono.just("ip:" + ip);
                }));
    }
}
