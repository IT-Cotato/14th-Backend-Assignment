package com.cotato.backend14th.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
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

import com.cotato.backend14th.domain.user.dto.LoginRequest;
import com.cotato.backend14th.domain.user.dto.LoginResponse;
import com.cotato.backend14th.domain.user.dto.SignupRequest;
import com.cotato.backend14th.domain.user.dto.SignupResponse;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtProvider;
import com.cotato.backend14th.global.security.TokenBlacklist;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private TokenBlacklist tokenBlacklist;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "user@gmail.com";
    private static final String RAW_PASSWORD = "password1234";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded";

    private SignupRequest signupRequest() {
        return new SignupRequest(EMAIL, RAW_PASSWORD, "홍길동", "길동이", 14, Part.BE);
    }

    private User savedUser(Long id) {
        User user = User.builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .name("홍길동")
                .nickname("길동이")
                .generation(14)
                .part(Part.BE)
                .build();
        // id는 DB가 채우는 값이라 테스트에서만 직접 넣어요
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("회원가입하면 비밀번호를 암호화해서 저장하고 userId를 돌려줘요")
    void signup_success() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.save(any(User.class))).willReturn(savedUser(1L));

        SignupResponse response = authService.signup(signupRequest());

        assertThat(response.getUserId()).isEqualTo(1L);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD).isNotEqualTo(RAW_PASSWORD);
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getGeneration()).isEqualTo(14);
        assertThat(saved.getPart()).isEqualTo(Part.BE);
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS를 던지고 저장하지 않아요")
    void signup_duplicateEmail() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> authService.signup(signupRequest()))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("이메일과 비밀번호가 맞으면 Bearer accessToken을 발급해요")
    void login_success() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(savedUser(1L)));
        given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(jwtProvider.createAccessToken(1L)).willReturn("access.token.value");

        LoginResponse response = authService.login(new LoginRequest(EMAIL, RAW_PASSWORD));

        assertThat(response.getAccessToken()).isEqualTo("access.token.value");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 INVALID_CREDENTIALS를 던져요")
    void login_unknownEmail() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 INVALID_CREDENTIALS를 던지고 토큰을 만들지 않아요")
    void login_wrongPassword() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(savedUser(1L)));
        given(passwordEncoder.matches("wrong-password", ENCODED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "wrong-password")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verify(jwtProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("로그아웃하면 토큰을 만료 시각과 함께 블랙리스트에 등록해요")
    void logout_addsTokenToBlacklist() {
        Instant expiresAt = Instant.parse("2026-11-23T13:00:00Z");
        given(jwtProvider.getExpiration("access.token.value")).willReturn(expiresAt);

        authService.logout("access.token.value");

        verify(tokenBlacklist).add("access.token.value", expiresAt);
    }
}
