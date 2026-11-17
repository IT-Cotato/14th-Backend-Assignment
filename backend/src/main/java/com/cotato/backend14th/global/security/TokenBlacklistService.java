package com.cotato.backend14th.global.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// 로그아웃한 토큰을 메모리에만 보관해요(인메모리로 결정).
// 서버가 재시작되면 블랙리스트가 비워지는 건 감수한 트레이드오프예요.
@Component
public class TokenBlacklistService {

    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token, Instant expiresAt) {
        blacklist.put(token, expiresAt);
    }

    public boolean isBlacklisted(String token) {
        Instant expiresAt = blacklist.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
