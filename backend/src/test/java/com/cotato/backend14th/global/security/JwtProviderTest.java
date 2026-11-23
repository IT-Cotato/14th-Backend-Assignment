package com.cotato.backend14th.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long!!";
    private static final String OTHER_SECRET = "another-secret-key-also-at-least-32-bytes-long!!";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 3_600_000L);

    @Test
    @DisplayName("발급한 토큰은 유효하고 userId를 다시 꺼낼 수 있어요")
    void createAndParse() {
        String token = jwtProvider.createAccessToken(7L);

        assertThat(jwtProvider.validate(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(7L);
        assertThat(jwtProvider.getExpiration(token)).isAfter(Instant.now());
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않아요")
    void expiredToken() {
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1_000L);
        String token = expiredProvider.createAccessToken(1L);

        assertThat(jwtProvider.validate(token)).isFalse();
    }

    @Test
    @DisplayName("서명이 변조된 토큰은 유효하지 않아요")
    void tamperedToken() {
        String token = jwtProvider.createAccessToken(1L);
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertThat(jwtProvider.validate(tampered)).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰은 유효하지 않아요")
    void tokenSignedWithOtherKey() {
        String token = new JwtProvider(OTHER_SECRET, 3_600_000L).createAccessToken(1L);

        assertThat(jwtProvider.validate(token)).isFalse();
    }

    @Test
    @DisplayName("형식이 아닌 문자열은 유효하지 않아요")
    void malformedToken() {
        assertThat(jwtProvider.validate("not-a-jwt")).isFalse();
        assertThat(jwtProvider.validate("")).isFalse();
    }

    @Test
    @DisplayName("Authorization 헤더에서 Bearer 토큰만 꺼내요")
    void resolveToken() {
        MockHttpServletRequest bearer = new MockHttpServletRequest();
        bearer.addHeader("Authorization", "Bearer abc.def.ghi");
        MockHttpServletRequest noHeader = new MockHttpServletRequest();
        MockHttpServletRequest basic = new MockHttpServletRequest();
        basic.addHeader("Authorization", "Basic xxx");

        assertThat(jwtProvider.resolveToken(bearer)).isEqualTo("abc.def.ghi");
        assertThat(jwtProvider.resolveToken(noHeader)).isNull();
        assertThat(jwtProvider.resolveToken(basic)).isNull();
    }
}
