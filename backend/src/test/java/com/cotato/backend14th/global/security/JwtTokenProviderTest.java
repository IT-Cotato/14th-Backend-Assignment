package com.cotato.backend14th.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("test-secret-key-for-jwt-token-provider-unit-test".repeat(2).getBytes());

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 3600000L);
    }

    @Test
    void generateToken_andParse_returnsSameUserId() {
        String token = jwtTokenProvider.generateToken(42L);

        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(42L);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(SECRET, -1000L);
        String token = expiredTokenProvider.generateToken(1L);

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("not-a-valid-token")).isFalse();
    }

    @Test
    void validateToken_signedWithDifferentKey_returnsFalse() {
        String differentSecret = Base64.getEncoder()
                .encodeToString("another-secret-key-for-unit-test-only".repeat(2).getBytes());
        JwtTokenProvider otherProvider = new JwtTokenProvider(differentSecret, 3600000L);
        String token = otherProvider.generateToken(1L);

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }
}
