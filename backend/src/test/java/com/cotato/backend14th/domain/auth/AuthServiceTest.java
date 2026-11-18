package com.cotato.backend14th.domain.auth;

import static com.cotato.backend14th.support.Fixtures.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.Map;
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
import com.cotato.backend14th.domain.auth.service.AuthService;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.jwt.JwtProvider;
import com.cotato.backend14th.global.jwt.TokenBlacklist;
import com.cotato.backend14th.support.Fixtures;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtProvider jwtProvider;
    @Mock
    TokenBlacklist tokenBlacklist;
    @InjectMocks
    AuthService service;

    private SignupRequest signupRequest() {
        return Fixtures.request(SignupRequest.class, Map.of(
                "email", "user@test.com", "password", "dummy-pass-1", "name", "홍길동",
                "nickname", "길동이", "generation", 14, "part", Part.BE));
    }

    private LoginRequest loginRequest(String password) {
        return Fixtures.request(LoginRequest.class, Map.of("email", "user@test.com", "password", password));
    }

    @Test
    @DisplayName("가입하면 비밀번호는 암호화된 값으로 저장되고, 기수와 파트가 함께 저장된다")
    void signup() {
        given(userRepository.existsByEmail("user@test.com")).willReturn(false);
        given(passwordEncoder.encode("dummy-pass-1")).willReturn("ENCODED");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });

        SignupResponse response = service.signup(signupRequest());

        assertThat(response.getUserId()).isEqualTo(7L);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("ENCODED").isNotEqualTo("dummy-pass-1");
        assertThat(saved.getGeneration()).isEqualTo(14);
        assertThat(saved.getPart()).isEqualTo(Part.BE);
        assertThat(saved.getNickname()).isEqualTo("길동이");
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS이고 저장하지 않는다")
    void signupWithDuplicateEmail() {
        given(userRepository.existsByEmail("user@test.com")).willReturn(true);

        assertErrorCode(() -> service.signup(signupRequest()), ErrorCode.EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("이메일과 비밀번호가 맞으면 토큰을 발급한다")
    void login() {
        User user = Fixtures.user(7L, "길동이", Part.BE);
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("dummy-pass-1", user.getPassword())).willReturn(true);
        given(jwtProvider.createToken(7L)).willReturn("jwt-token");

        LoginResponse response = service.login(loginRequest("dummy-pass-1"));

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("없는 이메일과 틀린 비밀번호는 같은 INVALID_CREDENTIALS이다 (어느 쪽이 틀렸는지 알려주지 않는다)")
    void loginFailures() {
        User user = Fixtures.user(7L, "길동이", Part.BE);
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.empty(), Optional.of(user));
        given(passwordEncoder.matches("wrong-pass-1", user.getPassword())).willReturn(false);

        assertErrorCode(() -> service.login(loginRequest("dummy-pass-1")), ErrorCode.INVALID_CREDENTIALS);
        assertErrorCode(() -> service.login(loginRequest("wrong-pass-1")), ErrorCode.INVALID_CREDENTIALS);
        verify(jwtProvider, never()).createToken(any());
    }

    @Test
    @DisplayName("로그아웃하면 토큰이 만료 시각과 함께 차단 목록에 들어간다")
    void logout() {
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        given(jwtProvider.getExpiration("jwt-token")).willReturn(expiration);

        service.logout("jwt-token");

        verify(tokenBlacklist).add("jwt-token", expiration);
    }
}
