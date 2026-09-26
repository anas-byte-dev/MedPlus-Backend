package com.medpulse.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medpulseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MedPulse AI — Autonomous Clinical Triage & Diagnostic Intelligence API")
                        .description("High-acuity clinical decision support, vital signs anomaly detection, ESI triage scoring, and agentic AI tools powered by Spring Boot 3 & Java 21.")
                        .version("2.5.0")
                        .contact(new Contact().name("MedPulse Clinical Engineering Team").email("clinical@medpulse.ai"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components().addSecuritySchemes("Bearer Authentication", createSecurityScheme()));
    }

    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("Enter your JWT Bearer token obtained from /api/auth/login");
    }
}
