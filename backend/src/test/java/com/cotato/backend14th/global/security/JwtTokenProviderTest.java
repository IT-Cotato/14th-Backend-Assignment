package com.cotato.backend14th.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-only-secret-key-for-unit-tests-0123456789";

    @Test
    void 발급한_토큰에서_회원_id를_꺼낸다() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 60_000));

        String token = provider.createAccessToken(7L);

        assertThat(provider.parse(token)).hasValueSatisfying(info -> assertThat(info.userId()).isEqualTo(7L));
    }

    @Test
    void 같은_회원이라도_매번_다른_토큰을_발급한다() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 60_000));

        assertThat(provider.createAccessToken(1L)).isNotEqualTo(provider.createAccessToken(1L));
    }

    @Test
    void 만료된_토큰은_거절한다() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, -1_000));

        assertThat(provider.parse(provider.createAccessToken(1L))).isEmpty();
    }

    @Test
    void 다른_키로_서명한_토큰은_거절한다() {
        JwtTokenProvider other = new JwtTokenProvider(new JwtProperties(SECRET + "-other", 60_000));
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 60_000));

        assertThat(provider.parse(other.createAccessToken(1L))).isEmpty();
        assertThat(provider.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void 시크릿이_32바이트보다_짧으면_시작하지_않는다() {
        assertThatThrownBy(() -> new JwtTokenProvider(new JwtProperties("short", 60_000)))
                .isInstanceOf(IllegalStateException.class);
    }
}
