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

import com.cotato.backend14th.global.security.JwtAuthenticationEntryPoint;
import com.cotato.backend14th.global.security.JwtAuthenticationFilter;
import com.cotato.backend14th.global.security.JwtTokenProvider;
import com.cotato.backend14th.global.security.TokenBlacklist;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // 앞서 만든 CorsConfig를 그대로 사용해요
                .csrf(AbstractHttpConfigurer::disable) // REST API는 세션 기반이 아니라 CSRF 보호가 필요 없어요
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션을 사용하지 않아요
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 권한은 docs/api-spec.md 4장을 따라요. 작성자 확인(403)은 각 service에서 해요.
                .authorizeHttpRequests(auth -> auth
                        // 공개: 회원가입, 로그인
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                        // 공개: 모집글 목록(6), 상세(7), 댓글 목록(11)
                        .requestMatchers(HttpMethod.GET,
                                "/api/study-posts",
                                "/api/study-posts/*",
                                "/api/study-posts/*/comments").permitAll()
                        // 3주차 예제와 Swagger, 에러 페이지
                        .requestMatchers("/hello", "/books/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 로그인: 로그아웃(3), 내 정보(4), 모집글 작성·수정·삭제(5, 8, 9), 댓글 작성·수정·삭제(10, 12, 13)
                        .anyRequest().authenticated()
                )
                // 인증이 필요한 API를 토큰 없이(또는 무효 토큰으로) 호출하면 ErrorResponse 형식의 401을 내려줘요
                .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklist),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
