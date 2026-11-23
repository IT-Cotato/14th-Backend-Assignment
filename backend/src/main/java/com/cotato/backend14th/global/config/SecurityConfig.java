// SecurityConfig.java
package com.cotato.backend14th.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import com.cotato.backend14th.global.security.JwtAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

import java.util.Set;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 로그인 없이 접근할 수 있는 Swagger 경로예요.
    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    // 토큰을 읽지 않는 경로예요.
    // 만료된 토큰이 남아 있어도 회원가입·로그인이 401로 막히지 않게 해요.
    private static final Set<String> TOKEN_IGNORED_PATHS = Set.of(
            "/api/auth/signup",
            "/api/auth/login"
    );

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // 앞서 만든 CorsConfig를 그대로 사용해요
                .csrf(AbstractHttpConfigurer::disable) // REST API는 세션 기반이 아니라 CSRF 보호가 필요 없어요
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션을 사용하지 않아요
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 회원가입, 로그인은 공개해요
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                        // Swagger 문서는 공개해요
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        // 모집글 목록·상세, 댓글 목록 조회는 공개해요
                        .requestMatchers(HttpMethod.GET,
                                "/api/study-posts",
                                "/api/study-posts/*",
                                "/api/study-posts/*/comments"
                        ).permitAll()
                        // 1~4주차 기존 API(BookController, HelloController)는 그대로 공개해요
                        .requestMatchers(HttpMethod.GET, "/books/*", "/hello").permitAll()
                        .requestMatchers(HttpMethod.POST, "/books").permitAll()
                        // 에러 응답 경로가 401로 덮이지 않게 열어둬요
                        .requestMatchers("/error").permitAll()
                        // 나머지는 모두 로그인이 필요해요
                        .anyRequest().authenticated()
                )
                // JwtConfig의 JwtDecoder로 서명과 만료 시간을 검증해요
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .jwt(jwt -> {
                        })
                )
                // 토큰이 아예 없을 때도 같은 401 형식으로 응답해요
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                );
        return http.build();
    }

    private BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        return request -> {
            if (TOKEN_IGNORED_PATHS.contains(request.getRequestURI())) {
                return null;
            }
            return defaultResolver.resolve(request);
        };
    }
}
