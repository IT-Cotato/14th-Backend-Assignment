package com.cotato.backend14th.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    // 테스트용 시크릿은 코드에 적지 않고 실행할 때마다 새로 만들어요
    private final String secret = UUID.randomUUID().toString() + UUID.randomUUID();

    @Test
    @DisplayName("발급한 토큰에서 userId와 만료 시각을 꺼낼 수 있다")
    void createAndParse() {
        JwtProvider jwtProvider = new JwtProvider(secret, 60_000L);

        String token = jwtProvider.createAccessToken(7L);

        assertThat(jwtProvider.getUserId(token)).contains(7L);
        assertThat(jwtProvider.getExpiresAt(token)).isPresent();
    }

    @Test
    @DisplayName("만료된 토큰은 userId를 돌려주지 않는다")
    void expiredToken() {
        JwtProvider jwtProvider = new JwtProvider(secret, -1_000L);

        String token = jwtProvider.createAccessToken(7L);

        assertThat(jwtProvider.getUserId(token)).isEmpty();
    }

    @Test
    @DisplayName("다른 시크릿으로 서명한 토큰이나 형식이 틀린 토큰은 userId를 돌려주지 않는다")
    void invalidToken() {
        JwtProvider jwtProvider = new JwtProvider(secret, 60_000L);
        JwtProvider otherProvider = new JwtProvider(UUID.randomUUID().toString() + UUID.randomUUID(), 60_000L);

        assertThat(jwtProvider.getUserId(otherProvider.createAccessToken(7L))).isEmpty();
        assertThat(jwtProvider.getUserId("not-a-jwt")).isEmpty();
    }
}
