package com.cotato.backend14th.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds}") long expirationSeconds
    ) {
        // HS256은 256비트(32바이트) 이상의 키가 필요해요. 짧으면 여기서 바로 실패해요.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    // 토큰마다 고유한 jti를 넣어서 로그아웃한 토큰을 구분할 수 있게 해요
    public String createToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    // 서명이 틀렸거나, 만료됐거나, 형식이 잘못된 토큰은 모두 UNAUTHORIZED로 처리해요
    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new ParsedToken(
                    Long.valueOf(claims.getSubject()),
                    claims.getId(),
                    LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault())
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new Backend14thException(ErrorCode.UNAUTHORIZED);
        }
    }

    public record ParsedToken(Long userId, String jti, LocalDateTime expiresAt) {
    }
}
