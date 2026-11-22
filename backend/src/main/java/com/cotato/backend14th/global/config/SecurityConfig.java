// SecurityConfig.java
package com.cotato.backend14th.global.config;

import com.cotato.backend14th.global.common.response.ErrorResponse;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain jwtFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        var paths = PathPatternRequestMatcher.withDefaults();
        AuthenticationEntryPoint unauthorized = (request, response, exception) -> {
            ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
            response.setStatus(errorCode.getHttpStatus().value());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(errorCode, request));
        };
        return http
                .securityMatcher(new OrRequestMatcher(
                        paths.matcher(HttpMethod.POST, "/api/auth/logout"),
                        paths.matcher(HttpMethod.POST, "/api/study-posts"),
                        paths.matcher(HttpMethod.POST, "/api/study-posts/{postId}/comments"),
                        paths.matcher(HttpMethod.PATCH, "/api/study-posts/{postId}"),
                        paths.matcher(HttpMethod.PATCH, "/api/comments/{commentId}"),
                        paths.matcher(HttpMethod.DELETE, "/api/comments/{commentId}"),
                        paths.matcher(HttpMethod.DELETE, "/api/study-posts/{postId}"),
                        paths.matcher(HttpMethod.GET, "/api/users/me"),
                        paths.matcher(HttpMethod.HEAD, "/api/users/me")))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(unauthorized))
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // 앞서 만든 CorsConfig를 그대로 사용해요
                .csrf(AbstractHttpConfigurer::disable) // REST API는 세션 기반이 아니라 CSRF 보호가 필요 없어요
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션을 사용하지 않아요
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                        .anyRequest().permitAll() // 이번 주는 전부 열어둬요
                );
        return http.build();
    }
}