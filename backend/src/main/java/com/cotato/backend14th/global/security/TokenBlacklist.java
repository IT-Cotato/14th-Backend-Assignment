package com.cotato.backend14th.global.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// 로그아웃한 토큰을 만료 시각까지 막아두는 인메모리 저장소예요 (서버를 재시작하면 비워져요)
@Component
public class TokenBlacklist {

    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();

    public void add(String token, Instant expiresAt) {
        Instant now = Instant.now();
        tokens.values().removeIf(expiry -> expiry.isBefore(now)); // 만료된 항목은 저장할 때 같이 정리해요
        tokens.put(token, expiresAt);
    }

    public boolean contains(String token) {
        Instant expiresAt = tokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            tokens.remove(token);
            return false;
        }
        return true;
    }
}
