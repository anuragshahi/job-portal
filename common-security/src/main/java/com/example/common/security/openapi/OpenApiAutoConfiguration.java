package com.example.common.security.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for OpenAPI documentation with JWT security scheme.
 * <p>
 * Automatically configures OpenAPI with:
 * <ul>
 *   <li>API title derived from spring.application.name (formatted as "Service Name API")</li>
 *   <li>Bearer JWT authentication security scheme</li>
 * </ul>
 * <p>
 * This auto-configuration is skipped if an {@link OpenAPI} bean already exists,
 * allowing services to provide custom OpenAPI configuration when needed.
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class OpenApiAutoConfiguration {

    @Value("${spring.application.name:application}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI openAPI() {
        String title = formatTitle(applicationName);
        return OpenApiConfigFactory.create(title);
    }

    /**
     * Formats the application name into a human-readable API title.
     * Examples:
     *   "profile-service" → "Profile Service API"
     *   "order-service" → "Order Service API"
     *   "bff" → "Bff API"
     */
    private String formatTitle(String appName) {
        if (appName == null || appName.isBlank()) {
            return "API";
        }

        String[] parts = appName.split("-");
        StringBuilder title = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                title.append(Character.toUpperCase(part.charAt(0)))
                     .append(part.substring(1))
                     .append(" ");
            }
        }
        return title.toString().trim() + " API";
    }
}
