package com.cotato.backend14th.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenBlacklistTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    private final TokenBlacklist blacklist = new TokenBlacklist(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("만료 전인 토큰을 등록하면 차단 목록에 있어요")
    void containsRegisteredToken() {
        blacklist.add("token", NOW.plus(Duration.ofMinutes(30)));

        assertThat(blacklist.contains("token")).isTrue();
        assertThat(blacklist.contains("other")).isFalse();
    }

    @Test
    @DisplayName("만료 시각이 지난 토큰은 차단 목록에서 빠지고 정리돼요")
    void expiredTokenIsRemoved() {
        blacklist.add("expired", NOW.minusSeconds(1));
        assertThat(blacklist.contains("expired")).isFalse();

        blacklist.add("expired-2", NOW.minusSeconds(1));
        blacklist.add("valid", NOW.plusSeconds(60)); // 등록할 때 만료된 항목을 정리해요

        assertThat(blacklist.size()).isEqualTo(1);
        assertThat(blacklist.contains("valid")).isTrue();
    }
}
