package com.cotato.backend14th.global.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// 로그아웃한 토큰을 만료 시각까지 기억하는 인메모리 저장소예요.
// 한계: 서버를 재시작하면 목록이 사라져서, 로그아웃한 토큰이 만료 전까지 다시 유효해져요.
@Component
public class TokenBlacklist {

    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBlacklist() {
        this(Clock.systemUTC());
    }

    TokenBlacklist(Clock clock) {
        this.clock = clock;
    }

    public void add(String token, Instant expiration) {
        removeExpired();
        tokens.put(token, expiration);
    }

    public boolean contains(String token) {
        Instant expiration = tokens.get(token);
        if (expiration == null) {
            return false;
        }
        if (!expiration.isAfter(clock.instant())) {
            // 이미 만료된 토큰은 JWT 검증에서 걸러지므로 목록에서 지워요
            tokens.remove(token);
            return false;
        }
        return true;
    }

    private void removeExpired() {
        Instant now = clock.instant();
        tokens.values().removeIf(expiration -> !expiration.isAfter(now));
    }
}
