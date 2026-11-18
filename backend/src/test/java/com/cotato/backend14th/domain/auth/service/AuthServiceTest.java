package com.cotato.backend14th.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.auth.entity.RevokedToken;
import com.cotato.backend14th.domain.auth.repository.RevokedTokenRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtProvider;
import com.cotato.backend14th.global.security.JwtProvider.ParsedToken;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "raw-password-for-test";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private static void assertErrorCode(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(Backend14thException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private static User userWithId(Long id) {
        User user = User.create(EMAIL, ENCODED_PASSWORD, "tester", "tester1", 14, Part.BE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Nested
    @DisplayName("회원가입")
    class Signup {

        private final SignupRequest request =
                new SignupRequest(EMAIL, RAW_PASSWORD, "tester", "tester1", 14, Part.BE);

        @Test
        @DisplayName("비밀번호를 암호화해서 저장하고 사용자 ID를 반환한다")
        void savesUserWithEncodedPassword() {
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
            given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            SignupResponse response = authService.signup(request);

            assertThat(response.getUserId()).isEqualTo(1L);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).saveAndFlush(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD).isNotEqualTo(RAW_PASSWORD);
            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.getGeneration()).isEqualTo(14);
            assertThat(saved.getPart()).isEqualTo(Part.BE);
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS이고 저장하지 않는다")
        void rejectsDuplicateEmail() {
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            assertErrorCode(() -> authService.signup(request), ErrorCode.EMAIL_ALREADY_EXISTS);

            verify(userRepository, never()).saveAndFlush(any(User.class));
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("동시 가입으로 DB의 UK 제약에 걸려도 EMAIL_ALREADY_EXISTS로 변환한다")
        void convertsUniqueViolationToEmailAlreadyExists() {
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
            given(userRepository.saveAndFlush(any(User.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate"));

            assertErrorCode(() -> authService.signup(request), ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        private final LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);

        @Test
        @DisplayName("이메일과 비밀번호가 맞으면 Bearer 토큰을 발급한다")
        void issuesToken() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(userWithId(1L)));
            given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(jwtProvider.createToken(1L)).willReturn("issued-token");

            LoginResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("issued-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("가입되지 않은 이메일이면 INVALID_CREDENTIALS이다")
        void rejectsUnknownEmail() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

            assertErrorCode(() -> authService.login(request), ErrorCode.INVALID_CREDENTIALS);

            verify(jwtProvider, never()).createToken(anyLong());
        }

        @Test
        @DisplayName("비밀번호가 틀리면 이메일이 틀렸을 때와 같은 INVALID_CREDENTIALS이다")
        void rejectsWrongPassword() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(userWithId(1L)));
            given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(false);

            assertErrorCode(() -> authService.login(request), ErrorCode.INVALID_CREDENTIALS);

            verify(jwtProvider, never()).createToken(anyLong());
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        private final LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        @Test
        @DisplayName("토큰의 jti와 만료 시각을 저장하고 만료된 기록을 정리한다")
        void revokesToken() {
            given(jwtProvider.parse("valid-token")).willReturn(new ParsedToken(1L, "jti-1", expiresAt));
            given(revokedTokenRepository.existsByJti("jti-1")).willReturn(false);

            authService.logout("valid-token");

            ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
            verify(revokedTokenRepository).save(captor.capture());
            assertThat(captor.getValue().getJti()).isEqualTo("jti-1");
            assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
            verify(revokedTokenRepository).deleteExpired(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("이미 무효 처리된 토큰은 다시 저장하지 않는다")
        void doesNotSaveTwice() {
            given(jwtProvider.parse("valid-token")).willReturn(new ParsedToken(1L, "jti-1", expiresAt));
            given(revokedTokenRepository.existsByJti("jti-1")).willReturn(true);

            authService.logout("valid-token");

            verify(revokedTokenRepository, never()).save(any(RevokedToken.class));
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 UNAUTHORIZED이고 아무것도 저장하지 않는다")
        void rejectsInvalidToken() {
            given(jwtProvider.parse("bad-token")).willThrow(new Backend14thException(ErrorCode.UNAUTHORIZED));

            assertErrorCode(() -> authService.logout("bad-token"), ErrorCode.UNAUTHORIZED);

            verifyNoInteractions(revokedTokenRepository);
        }
    }
}
