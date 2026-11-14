package com.cotato.backend14th.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenBlacklistTest {

    private static final Instant NOW = Instant.parse("2026-11-14T00:00:00Z");

    private final TokenBlacklist tokenBlacklist = new TokenBlacklist(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("만료 시각 전까지는 무효 목록에 남아 있다")
    void contains_beforeExpiration() {
        tokenBlacklist.add("token", NOW.plusSeconds(60));

        assertThat(tokenBlacklist.contains("token")).isTrue();
        assertThat(tokenBlacklist.contains("other-token")).isFalse();
    }

    @Test
    @DisplayName("만료 시각이 지난 토큰은 목록에서 정리된다")
    void contains_afterExpiration() {
        tokenBlacklist.add("expired", NOW.minusSeconds(1));

        assertThat(tokenBlacklist.contains("expired")).isFalse();
    }
}
