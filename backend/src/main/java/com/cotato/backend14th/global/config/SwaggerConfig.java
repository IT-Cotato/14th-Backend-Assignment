package com.cotato.backend14th.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("코테이토 스터디 매칭 API").version("v1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    OpenApiCustomizer protectedRoutes() {
        return api -> api.getPaths().forEach((path, item) -> {
            if (!path.startsWith("/api/") || path.equals("/api/auth/signup") || path.equals("/api/auth/login")) {
                return;
            }
            item.readOperationsMap().forEach((method, operation) -> {
                if (method != io.swagger.v3.oas.models.PathItem.HttpMethod.GET || !path.startsWith("/api/study-posts")) {
                    operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                }
            });
        });
    }
}
