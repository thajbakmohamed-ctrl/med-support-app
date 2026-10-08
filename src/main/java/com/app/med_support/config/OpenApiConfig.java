package com.app.med_support.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        String securitySchemeName = "bearerAuth";

        return new OpenAPI().info(new Info().title("Med Support API")
        .version("1.0").description("REST API for the Med Support blood donation platform. " +
        "The system connects donors with hospitals that require blood donations. " +
        "It supports user authentication, donor profiles, hospital management, " +
        "blood requests, donation bookings, profile management, and real-time booking updates."))
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(
                 securitySchemeName, new SecurityScheme().name(securitySchemeName)
                 .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                 .description("Enter the JWT token received after login.")));
    }
}