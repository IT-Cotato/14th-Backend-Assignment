// SecurityConfig.java
package com.cotato.backend14th.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.cotato.backend14th.global.security.CustomAccessDeniedHandler;
import com.cotato.backend14th.global.security.CustomAuthenticationEntryPoint;
import com.cotato.backend14th.global.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // 앞서 만든 CorsConfig를 그대로 사용해요
                .csrf(AbstractHttpConfigurer::disable) // REST API는 세션 기반이 아니라 CSRF 보호가 필요 없어요
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션을 사용하지 않아요
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers("/api/users/me", "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/study-posts").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/study-posts/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/study-posts/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/study-posts/*/comments").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()
                        .anyRequest().permitAll() // 그 외(book, swagger, 모집글/댓글 목록·상세 등) 기존 공개 범위는 그대로 둬요
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}