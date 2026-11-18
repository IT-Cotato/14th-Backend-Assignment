package com.cotato.backend14th.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 테스트용 임시 시크릿이다. (실제 시크릿은 .env의 JWT_SECRET에 있고 코드에 적지 않는다)
class JwtProviderTest {

    private static final String SECRET = "unit-test-only-secret-key-unit-test-only-secret-key";

    private final JwtProvider provider = new JwtProvider(SECRET, 60_000);

    @Test
    @DisplayName("발급한 토큰은 유효하고, 안에서 사용자 id를 꺼낼 수 있다")
    void createAndParse() {
        String token = provider.createToken(42L);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(42L);
        assertThat(provider.getExpiration(token)).isAfter(new Date());
    }

    @Test
    @DisplayName("이미 만료된 토큰은 유효하지 않다")
    void expiredToken() {
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1_000);

        assertThat(expiredProvider.validateToken(expiredProvider.createToken(1L))).isFalse();
    }

    // 서명(마지막 조각)의 첫 글자를 다른 글자로 바꿔서 변조한 토큰을 만든다
    private String tamperSignature(String token) {
        int signatureStart = token.lastIndexOf('.') + 1;
        char first = token.charAt(signatureStart);
        char replaced = first == 'A' ? 'B' : 'A';
        return token.substring(0, signatureStart) + replaced + token.substring(signatureStart + 1);
    }

    @Test
    @DisplayName("값이 바뀐 토큰, 다른 키로 만든 토큰, 엉뚱한 문자열은 유효하지 않다")
    void invalidTokens() {
        String token = provider.createToken(1L);
        JwtProvider otherKeyProvider = new JwtProvider("another-unit-test-secret-key-another-unit-test-secret", 60_000);

        assertThat(provider.validateToken(tamperSignature(token))).isFalse();
        assertThat(provider.validateToken(otherKeyProvider.createToken(1L))).isFalse();
        assertThat(provider.validateToken("not-a-token")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
    }
}
