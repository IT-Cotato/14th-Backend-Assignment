package com.cotato.backend14th.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expirationMs;

    // @Value 어노테이션은 설정파일(application.yml) 혹은 (.env)에 작성한 값을 코드의 필드로 가져오는 어노테이션이다.

    public JwtProvider(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs){
        // 설정파일에 있는 secret을 받아서 서명에 사용할 수 있는 SecretKey 객체로 바꿔 key 필드에 보관한다.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String createToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token){
        try{
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token){
        return Long.valueOf(parse(token).getSubject());
    }

    private Claims parse(String token){
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
