package com.cotato.backend14th.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

// accessToken 발급과 검증을 담당해요. 토큰의 subject에는 userId를 넣어요.
@Component
public class JwtTokenProvider {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;
    private final long accessTokenExpirationMillis;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpirationMillis
    ) {
        // HS256은 32바이트 이상의 키가 필요해요. 짧으면 앱 시작 시점에 예외가 나요.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
    }

    public String createAccessToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMillis)))
                .signWith(key)
                .compact();
    }

    // 서명이 맞고 만료되지 않은 토큰이면 userId를 돌려줘요. 아니면 빈 값이에요.
    public Optional<Long> getUserId(String token) {
        return parseClaims(token).map(claims -> Long.valueOf(claims.getSubject()));
    }

    // 유효한 토큰의 만료 시각이에요. 유효하지 않으면 빈 값이에요.
    public Optional<Instant> getExpiration(String token) {
        return parseClaims(token).map(claims -> claims.getExpiration().toInstant());
    }

    // Authorization 헤더에서 "Bearer " 뒤의 토큰을 꺼내요.
    public Optional<String> resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private Optional<Claims> parseClaims(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
