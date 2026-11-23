package com.cotato.backend14th.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.auth.dto.TokenResponse;
import com.cotato.backend14th.domain.auth.entity.LogoutToken;
import com.cotato.backend14th.domain.auth.repository.LogoutTokenRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "raw-password";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private LogoutTokenRepository logoutTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입하면 비밀번호를 암호화해서 저장하고 userId를 돌려준다")
    void signup_success() {
        SignupRequest request = new SignupRequest(EMAIL, RAW_PASSWORD, "홍길동", "길동이", 14, Part.BE);
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        SignupResponse response = authService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(saved.getGeneration()).isEqualTo(14);
        assertThat(saved.getPart()).isEqualTo(Part.BE);
    }

    @Test
    @DisplayName("이미 가입된 이메일로 회원가입하면 EMAIL_ALREADY_EXISTS 예외가 난다")
    void signup_duplicateEmail() {
        SignupRequest request = new SignupRequest(EMAIL, RAW_PASSWORD, "홍길동", "길동이", 14, Part.BE);
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("이메일과 비밀번호가 맞으면 Bearer accessToken을 발급한다")
    void login_success() {
        User user = user(1L);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(jwtProvider.createAccessToken(1L)).willReturn("access-token");

        TokenResponse response = authService.login(new LoginRequest(EMAIL, RAW_PASSWORD));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 INVALID_CREDENTIALS 예외가 난다")
    void login_wrongPassword() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user(1L)));
        given(passwordEncoder.matches("wrong-password", ENCODED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "wrong-password")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("가입하지 않은 이메일로 로그인하면 INVALID_CREDENTIALS 예외가 난다")
    void login_unknownEmail() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("로그아웃하면 토큰을 만료 시각과 함께 저장한다")
    void logout_savesToken() {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 11, 19, 12, 0);
        given(logoutTokenRepository.existsByToken("access-token")).willReturn(false);
        given(jwtProvider.getExpiresAt("access-token")).willReturn(Optional.of(expiresAt));

        authService.logout("access-token");

        ArgumentCaptor<LogoutToken> captor = ArgumentCaptor.forClass(LogoutToken.class);
        verify(logoutTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("access-token");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("이미 로그아웃한 토큰은 다시 저장하지 않는다")
    void logout_alreadyLoggedOut() {
        given(logoutTokenRepository.existsByToken("access-token")).willReturn(true);

        authService.logout("access-token");

        verify(logoutTokenRepository, never()).save(any(LogoutToken.class));
    }

    private User user(Long id) {
        User user = User.builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .name("홍길동")
                .nickname("길동이")
                .generation(14)
                .part(Part.BE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
