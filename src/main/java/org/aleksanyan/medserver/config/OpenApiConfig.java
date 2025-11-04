package org.aleksanyan.medserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI medserverOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MedScheduler API")
                        .version("v1")
                        .description("""
                                API для MVP-приложения расписания лечения.
                                Используется JWT Bearer авторизация.
                                Сначала зарегистрируйтесь/войдите, затем нажмите Authorize и введите `Bearer <access_token>`.
                                """)
                        .license(new License().name("MIT"))
                        .contact(new Contact()
                                .name("Aleksanyan Evgeniy")
                                .email("grigalex92@gmail.com")))
                .servers(List.of(
                        new Server().url("/").description("Default")
                ))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    public GroupedOpenApi medserverGroup() {
        return GroupedOpenApi.builder()
                .group("medserver")
                .pathsToMatch("/api/**")
                .build();
    }
}
