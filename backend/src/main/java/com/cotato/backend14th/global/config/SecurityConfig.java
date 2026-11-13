package com.cotato.backend14th.global.config;

import com.cotato.backend14th.global.common.response.ErrorResponse;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, TokenService tokens, ObjectMapper mapper,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        return http.cors(config -> config.configurationSource(cors))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(config -> config.authenticationEntryPoint(
                        (request, response, error) -> unauthorized(request, response, mapper)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/study-posts", "/api/study-posts/{id}",
                                "/api/study-posts/{id}/comments").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected boolean shouldNotFilter(HttpServletRequest request) {
                        return !request.getServletPath().startsWith("/api/");
                    }

                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                            FilterChain chain) throws ServletException, IOException {
                        String header = request.getHeader("Authorization");
                        if (header != null) {
                            try {
                                if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                                    throw new org.springframework.security.oauth2.jwt.BadJwtException("Bearer token required");
                                }
                                var jwt = tokens.authenticate(header.substring(7));
                                SecurityContextHolder.getContext().setAuthentication(
                                        new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
                            } catch (JwtException | IllegalArgumentException error) {
                                unauthorized(request, response, mapper);
                                return;
                            }
                        }
                        chain.doFilter(request, response);
                    }
                }, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static void unauthorized(HttpServletRequest request, HttpServletResponse response, ObjectMapper mapper)
            throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), ErrorResponse.of(ErrorCode.UNAUTHORIZED, request));
    }
}
