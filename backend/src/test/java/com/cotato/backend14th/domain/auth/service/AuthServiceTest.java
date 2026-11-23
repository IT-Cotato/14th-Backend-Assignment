package com.cotato.backend14th.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;

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
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtTokenProvider;
import com.cotato.backend14th.global.security.JwtTokenProvider.TokenInfo;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    private static final String RAW_PASSWORD = "raw-password";

    private SignupRequest signupRequest(String email) {
        return new SignupRequest(email, RAW_PASSWORD, "홍길동", "길동이", 14, Part.BE);
    }

    private User savedUser(Long id) {
        User user = User.builder()
                .email("user@gmail.com").password("hashed").name("홍길동")
                .nickname("길동이").generation(14).part(Part.BE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void 회원가입하면_비밀번호를_암호화하고_기수와_파트를_저장한다() {
        given(userRepository.existsByEmail("user@gmail.com")).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn("hashed");
        given(userRepository.save(any(User.class))).willReturn(savedUser(1L));

        Long userId = authService.signup(signupRequest(" User@Gmail.com ")).userId();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(userId).isEqualTo(1L);
        assertThat(saved.getEmail()).isEqualTo("user@gmail.com");
        assertThat(saved.getPassword()).isEqualTo("hashed").isNotEqualTo(RAW_PASSWORD);
        assertThat(saved.getGeneration()).isEqualTo(14);
        assertThat(saved.getPart()).isEqualTo(Part.BE);
    }

    @Test
    void 이미_가입된_이메일이면_EMAIL_ALREADY_EXISTS() {
        given(userRepository.existsByEmail("user@gmail.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(signupRequest("user@gmail.com")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 로그인에_성공하면_Bearer_토큰을_발급한다() {
        given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(savedUser(1L)));
        given(passwordEncoder.matches(RAW_PASSWORD, "hashed")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");

        LoginResponse response = authService.login(new LoginRequest("user@gmail.com", RAW_PASSWORD));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void 비밀번호가_틀리면_INVALID_CREDENTIALS() {
        given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(savedUser(1L)));
        given(passwordEncoder.matches(anyString(), eq("hashed"))).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@gmail.com", "wrong-password")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 없는_이메일이어도_같은_INVALID_CREDENTIALS() {
        given(userRepository.findByEmail("none@gmail.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("none@gmail.com", RAW_PASSWORD)))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 로그아웃하면_토큰을_만료_시각까지_블랙리스트에_넣는다() {
        Instant expiresAt = Instant.now().plusSeconds(600);
        given(jwtTokenProvider.parse("access-token")).willReturn(Optional.of(new TokenInfo(1L, expiresAt)));

        authService.logout("access-token");

        verify(tokenBlacklistService).blacklist("access-token", expiresAt);
    }

    @Test
    void 무효한_토큰으로_로그아웃하면_UNAUTHORIZED() {
        given(jwtTokenProvider.parse("broken")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("broken"))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
