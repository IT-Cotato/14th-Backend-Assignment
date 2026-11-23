package com.cotato.backend14th.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenBlacklistTest {

    private final TokenBlacklist blacklist = new TokenBlacklist();

    private Date inFuture() {
        return new Date(System.currentTimeMillis() + 60_000);
    }

    @Test
    @DisplayName("등록한 토큰만 차단 목록에 있다")
    void addedTokenIsBlocked() {
        blacklist.add("logged-out", inFuture());

        assertThat(blacklist.isBlacklisted("logged-out")).isTrue();
        assertThat(blacklist.isBlacklisted("another-token")).isFalse();
    }

    @Test
    @DisplayName("만료 시각이 지난 토큰은 다음 등록 때 목록에서 지워진다")
    void expiredTokenIsRemoved() {
        blacklist.add("old-token", new Date(System.currentTimeMillis() - 1_000));

        blacklist.add("new-token", inFuture());

        assertThat(blacklist.isBlacklisted("old-token")).isFalse();
        assertThat(blacklist.isBlacklisted("new-token")).isTrue();
    }
}
