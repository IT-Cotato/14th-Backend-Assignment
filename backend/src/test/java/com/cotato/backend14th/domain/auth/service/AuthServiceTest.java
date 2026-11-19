package com.cotato.backend14th.domain.auth.service;

import static com.cotato.backend14th.support.TestFixtures.loginRequest;
import static com.cotato.backend14th.support.TestFixtures.signupRequest;
import static com.cotato.backend14th.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.util.ReflectionTestUtils.setField;

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

import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.auth.entity.TokenBlacklist;
import com.cotato.backend14th.domain.auth.repository.TokenBlacklistRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입하면 비밀번호를 암호화해서 저장하고 새 사용자 id를 돌려준다")
    void signup() {
        // given
        SignupRequest request = signupRequest("new@test.com", "plain-password", "홍길동", "길동", 14, Part.FE);
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        given(passwordEncoder.encode("plain-password")).willReturn("encoded-password");
        // 저장하면 DB가 id를 붙여 주는 것처럼 흉내 내요
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            setField(saved, "id", 1L);
            return saved;
        });

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getEmail()).isEqualTo("new@test.com");
        assertThat(saved.getNickname()).isEqualTo("길동");
        assertThat(saved.getGeneration()).isEqualTo(14);
        assertThat(saved.getPart()).isEqualTo(Part.FE);
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS 예외가 나고 저장하지 않는다")
    void signupWithDuplicateEmail() {
        // given
        SignupRequest request = signupRequest("dup@test.com", "plain-password", "홍길동", "길동", 14, Part.BE);
        given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이메일과 비밀번호가 맞으면 Bearer 액세스 토큰을 돌려준다")
    void login() {
        // given
        User user = user(1L);
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("plain-password", "encoded-password")).willReturn(true);
        given(jwtProvider.createAccessToken(1L)).willReturn("access-token");

        // when
        LoginResponse response = authService.login(loginRequest(user.getEmail(), "plain-password"));

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("없는 이메일로 로그인하면 INVALID_CREDENTIALS 예외가 난다")
    void loginWithUnknownEmail() {
        // given
        given(userRepository.findByEmail("none@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest("none@test.com", "plain-password")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        then(jwtProvider).should(never()).createAccessToken(anyLong());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 없는 이메일과 같은 INVALID_CREDENTIALS 예외가 난다")
    void loginWithWrongPassword() {
        // given
        User user = user(1L);
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        // when & then
        // 이메일이 가입돼 있는지 알려주지 않으려고 같은 에러 코드를 써요
        assertThatThrownBy(() -> authService.login(loginRequest(user.getEmail(), "wrong-password")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        then(jwtProvider).should(never()).createAccessToken(anyLong());
    }

    @Test
    @DisplayName("로그아웃하면 토큰과 만료 시각을 블랙리스트에 저장한다")
    void logout() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2026, 11, 19, 12, 0);
        given(jwtProvider.resolveToken("Bearer access-token")).willReturn("access-token");
        given(tokenBlacklistRepository.existsByToken("access-token")).willReturn(false);
        given(jwtProvider.getExpiresAt("access-token")).willReturn(expiresAt);

        // when
        authService.logout("Bearer access-token");

        // then
        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        then(tokenBlacklistRepository).should().save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("access-token");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("이미 블랙리스트에 있는 토큰이면 다시 저장하지 않는다")
    void logoutWithBlacklistedToken() {
        // given
        given(jwtProvider.resolveToken("Bearer access-token")).willReturn("access-token");
        given(tokenBlacklistRepository.existsByToken("access-token")).willReturn(true);

        // when
        authService.logout("Bearer access-token");

        // then
        then(tokenBlacklistRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아니면 UNAUTHORIZED 예외가 난다")
    void logoutWithoutBearerToken() {
        // given
        given(jwtProvider.resolveToken("Basic abc")).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.logout("Basic abc"))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        then(tokenBlacklistRepository).should(never()).save(any());
    }
}
