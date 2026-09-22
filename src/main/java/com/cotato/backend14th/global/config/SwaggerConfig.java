package com.cotato.backend14th.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("My API")                    // 문서 제목
                        .description("스터디 실습용 API 문서")  // 설명
                        .version("v1.0.0"));                  // 버전
    }
}
