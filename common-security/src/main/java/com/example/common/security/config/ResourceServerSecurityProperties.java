package com.example.common.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for resource server security.
 */
@ConfigurationProperties(prefix = "security.resource-server")
public class ResourceServerSecurityProperties {

    /**
     * Additional public endpoint patterns (permitAll).
     * Default patterns (/actuator/health, /actuator/prometheus) are always included.
     */
    private List<String> publicEndpoints = new ArrayList<>();

    public List<String> getPublicEndpoints() {
        return publicEndpoints;
    }

    public void setPublicEndpoints(List<String> publicEndpoints) {
        this.publicEndpoints = publicEndpoints;
    }
}
