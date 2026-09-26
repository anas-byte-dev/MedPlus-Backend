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
                        .title("MedPlus Healthcare API — Doctor Scheduling & Clinical Care Engine")
                        .description("REST API for doctor appointment scheduling, multi-role hospital access, and clinical triage evaluation built with Spring Boot 3 and Java.")
                        .version("1.0.0")
                        .contact(new Contact().name("Anas Siddiqui").email("anassidd7256@gmail.com"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components().addSecuritySchemes("Bearer Authentication", createSecurityScheme()));
    }

    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("Enter JWT Bearer token obtained from /api/auth/login");
    }
}
