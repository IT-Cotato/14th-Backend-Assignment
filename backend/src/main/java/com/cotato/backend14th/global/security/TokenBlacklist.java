package com.cotato.backend14th.global.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 로그아웃한 accessToken을 만료 시각까지 서버 메모리에 보관해요.
 * 테이블을 늘리지 않으려고 인메모리로 했고, 서버를 재시작하면 목록이 사라져요. (docs/erd/README.md 설계 메모)
 */
@Component
public class TokenBlacklist {

    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBlacklist() {
        this(Clock.systemUTC());
    }

    // 테스트에서 시간을 고정하려고 Clock을 받아요
    TokenBlacklist(Clock clock) {
        this.clock = clock;
    }

    public void add(String token, Instant expiresAt) {
        // 스케줄러 없이 메모리가 계속 늘지 않도록, 등록할 때마다 이미 만료된 토큰을 정리해요
        removeExpired();
        tokens.put(token, expiresAt);
    }

    public boolean contains(String token) {
        Instant expiresAt = tokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (!expiresAt.isAfter(clock.instant())) {
            tokens.remove(token);
            return false;
        }
        return true;
    }

    int size() {
        return tokens.size();
    }

    private void removeExpired() {
        Instant now = clock.instant();
        tokens.values().removeIf(expiresAt -> !expiresAt.isAfter(now));
    }
}
