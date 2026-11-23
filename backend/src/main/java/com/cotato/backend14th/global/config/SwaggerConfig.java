package com.cotato.backend14th.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // Swagger에서 Authorize 버튼으로 JWT를 넣을 때 쓰는 보안 스킴 이름이에요.
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("코테이토 스터디 매칭 API")
                        .description("코테이토 14기 스터디 매칭 플랫폼의 인증, 스터디 모집글, 댓글(스터디 신청) API 문서예요.")
                        .version("v1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
