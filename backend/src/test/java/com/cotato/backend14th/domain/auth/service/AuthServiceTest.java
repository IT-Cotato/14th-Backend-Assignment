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

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.entity.BlacklistedToken;
import com.cotato.backend14th.domain.auth.repository.BlacklistedTokenRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtTokenProvider;
import com.cotato.backend14th.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock BlacklistedTokenRepository blacklistedTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @InjectMocks AuthService authService;

    private final SignupRequest signupRequest =
            new SignupRequest("a@test.com", "rawPassword", "홍길동", "길동이", 14, Part.BE);

    @Test
    @DisplayName("회원가입하면 비밀번호를 BCrypt로 암호화해서 저장한다")
    void signup_encodesPassword() {
        given(userRepository.existsByEmail("a@test.com")).willReturn(false);
        given(passwordEncoder.encode("rawPassword")).willReturn("hashed");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        authService.signup(signupRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getGeneration()).isEqualTo(14);
        assertThat(captor.getValue().getPart()).isEqualTo(Part.BE);
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS")
    void signup_duplicateEmail() {
        given(userRepository.existsByEmail("a@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("이메일과 비밀번호가 맞으면 Bearer 토큰을 발급한다")
    void login_success() {
        User user = TestFixtures.user(1L, Part.BE);
        given(userRepository.findByEmail("a@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pw", "encoded")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("token");

        LoginResponse response = authService.login(new LoginRequest("a@test.com", "pw"));

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 INVALID_CREDENTIALS")
    void login_wrongPassword() {
        given(userRepository.findByEmail("a@test.com")).willReturn(Optional.of(TestFixtures.user(1L, Part.BE)));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@test.com", "wrong")))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("없는 이메일이면 INVALID_CREDENTIALS (이메일 존재 여부를 숨긴다)")
    void login_unknownEmail() {
        given(userRepository.findByEmail("none@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("none@test.com", "pw")))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("로그아웃하면 토큰을 만료 시각과 함께 블랙리스트에 저장한다")
    void logout_blacklistsToken() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        given(jwtTokenProvider.isValid("token")).willReturn(true);
        given(blacklistedTokenRepository.existsByToken("token")).willReturn(false);
        given(jwtTokenProvider.getExpiration("token")).willReturn(expiresAt);

        authService.logout("token");

        ArgumentCaptor<BlacklistedToken> captor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("token");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 로그아웃하면 UNAUTHORIZED")
    void logout_invalidToken() {
        given(jwtTokenProvider.isValid("bad")).willReturn(false);

        assertThatThrownBy(() -> authService.logout("bad"))
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
