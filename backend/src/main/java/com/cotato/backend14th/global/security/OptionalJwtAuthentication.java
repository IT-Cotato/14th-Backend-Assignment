package com.cotato.backend14th.global.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OptionalJwtAuthentication {

    private final JwtDecoder jwtDecoder;
    private final DefaultBearerTokenResolver tokenResolver = new DefaultBearerTokenResolver();

    public Jwt resolve(HttpServletRequest request) {
        try {
            String token = tokenResolver.resolve(request);
            return token == null ? null : jwtDecoder.decode(token);
        } catch (JwtException | OAuth2AuthenticationException e) {
            return null;
        }
    }
}
