package com.example.common.security.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

public class OpenApiConfigFactory {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    public static OpenAPI create(String title) {
        return create(title, "1.0");
    }

    public static OpenAPI create(String title, String version) {
        return new OpenAPI()
                .info(new Info().title(title).version(version))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
