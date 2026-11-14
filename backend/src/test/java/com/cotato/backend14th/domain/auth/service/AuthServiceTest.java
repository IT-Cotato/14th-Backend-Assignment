package com.cotato.backend14th.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtTokenProvider;
import com.cotato.backend14th.global.security.TokenBlacklist;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "user@gmail.com";
    private static final String RAW_PASSWORD = "password1234";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입에 성공하면 암호화한 비밀번호로 저장하고 userId를 돌려준다")
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

        assertThat(response.userId()).isEqualTo(1L);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getGeneration()).isEqualTo(14);
        assertThat(saved.getPart()).isEqualTo(Part.BE);
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS를 던지고 저장하지 않는다")
    void signup_duplicateEmail() {
        SignupRequest request = new SignupRequest(EMAIL, RAW_PASSWORD, "홍길동", "길동이", 14, Part.BE);
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("이메일과 비밀번호가 맞으면 Bearer accessToken을 발급한다")
    void login_success() {
        User user = savedUser(1L);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("issued-token");

        LoginResponse response = authService.login(new LoginRequest(EMAIL, RAW_PASSWORD));

        assertThat(response.accessToken()).isEqualTo("issued-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 INVALID_CREDENTIALS를 던지고 토큰을 발급하지 않는다")
    void login_wrongPassword() {
        User user = savedUser(1L);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", ENCODED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "wrong-password")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verify(jwtTokenProvider, never()).createAccessToken(anyLong());
    }

    @Test
    @DisplayName("가입하지 않은 이메일도 비밀번호 불일치와 같은 INVALID_CREDENTIALS를 던진다")
    void login_unknownEmail() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
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
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
