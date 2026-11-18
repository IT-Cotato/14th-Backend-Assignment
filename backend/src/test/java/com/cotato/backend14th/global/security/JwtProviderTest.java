package com.cotato.backend14th.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtProvider.ParsedToken;

import io.jsonwebtoken.security.WeakKeyException;

// Spring 없이 JwtProvider만 검증하는 테스트다. 키는 테스트 전용 가짜 값이다.
class JwtProviderTest {

    private static final String SECRET = "test-only-secret-key-for-jwt-provider-0123456789";
    private static final String OTHER_SECRET = "another-test-only-secret-key-for-jwt-9876543210";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 3600);

    private static void assertUnauthorized(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(Backend14thException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("발급한 토큰을 파싱하면 사용자 ID, jti, 미래의 만료 시각이 나온다")
    void createAndParse() {
        String token = jwtProvider.createToken(42L);

        ParsedToken parsed = jwtProvider.parse(token);

        assertThat(parsed.userId()).isEqualTo(42L);
        assertThat(parsed.jti()).isNotBlank();
        assertThat(parsed.expiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("토큰마다 jti가 다르다")
    void jtiIsUniquePerToken() {
        ParsedToken first = jwtProvider.parse(jwtProvider.createToken(1L));
        ParsedToken second = jwtProvider.parse(jwtProvider.createToken(1L));

        assertThat(first.jti()).isNotEqualTo(second.jti());
    }

    @Test
    @DisplayName("내용을 위조한 토큰은 UNAUTHORIZED이다")
    void rejectsTamperedToken() {
        String[] parts = jwtProvider.createToken(1L).split("\\.");
        String tampered = parts[0] + "." + parts[1] + "x." + parts[2];

        assertUnauthorized(() -> jwtProvider.parse(tampered));
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰은 UNAUTHORIZED이다")
    void rejectsTokenSignedWithOtherKey() {
        String token = new JwtProvider(OTHER_SECRET, 3600).createToken(1L);

        assertUnauthorized(() -> jwtProvider.parse(token));
    }

    @Test
    @DisplayName("만료된 토큰은 UNAUTHORIZED이다")
    void rejectsExpiredToken() {
        String expired = new JwtProvider(SECRET, -10).createToken(1L);

        assertUnauthorized(() -> jwtProvider.parse(expired));
    }

    @Test
    @DisplayName("형식이 아닌 문자열, 빈 문자열, null은 UNAUTHORIZED이다")
    void rejectsMalformedToken() {
        assertUnauthorized(() -> jwtProvider.parse("not.a.jwt"));
        assertUnauthorized(() -> jwtProvider.parse(""));
        assertUnauthorized(() -> jwtProvider.parse(null));
    }

    @Test
    @DisplayName("키가 32바이트보다 짧으면 생성 시점에 바로 실패한다")
    void failsFastOnShortSecret() {
        assertThatThrownBy(() -> new JwtProvider("too-short", 3600))
                .isInstanceOf(WeakKeyException.class);
    }
}
