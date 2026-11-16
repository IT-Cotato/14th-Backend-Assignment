package com.cotato.backend14th.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import io.jsonwebtoken.JwtException;

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
    private JwtProvider jwtProvider;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("중복되지 않은 이메일이면 비밀번호를 암호화해 저장하고 회원 id를 반환한다")
        void signup_success() {
            // given
            SignupRequest request = signupRequest();
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            // when
            SignupResponse response = authService.signup(request);

            // then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();

            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(saved.getName()).isEqualTo("홍길동");
            assertThat(saved.getNickname()).isEqualTo("길동이");
            assertThat(saved.getGeneration()).isEqualTo(14);
            assertThat(saved.getPart()).isEqualTo(Part.BE);
            assertThat(response.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS 예외가 발생하고 저장하지 않는다")
        void signup_duplicateEmail() {
            // given
            SignupRequest request = signupRequest();
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("이메일과 비밀번호가 맞으면 액세스 토큰을 발급한다")
        void login_success() {
            // given
            User user = savedUser(1L);
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(jwtProvider.createAccessToken(1L)).willReturn("access-token");

            // when
            LoginResponse response = authService.login(loginRequest(EMAIL, RAW_PASSWORD));

            // then
            assertThat(response.getAccessToken()).isEqualTo("access-token");
        }

        @Test
        @DisplayName("비밀번호가 틀리면 INVALID_CREDENTIALS 예외가 발생하고 토큰을 발급하지 않는다")
        void login_wrongPassword() {
            // given
            User user = savedUser(1L);
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong-password", ENCODED_PASSWORD)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(loginRequest(EMAIL, "wrong-password")))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(jwtProvider, never()).createAccessToken(anyLong());
        }

        @Test
        @DisplayName("가입되지 않은 이메일이면 INVALID_CREDENTIALS 예외가 발생한다")
        void login_emailNotFound() {
            // given
            given(userRepository.findByEmail("none@gmail.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(loginRequest("none@gmail.com", RAW_PASSWORD)))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtProvider, never()).createAccessToken(anyLong());
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("토큰을 만료 시각과 함께 블랙리스트에 추가한다")
        void logout_success() {
            // given
            Instant expiration = Instant.now().plusSeconds(3600);
            given(jwtProvider.getExpiration("access-token")).willReturn(expiration);

            // when
            authService.logout("access-token");

            // then
            verify(tokenBlacklist).add("access-token", expiration);
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 JwtException이 전파되고 블랙리스트에 추가하지 않는다")
        void logout_invalidToken() {
            // given
            given(jwtProvider.getExpiration("invalid-token")).willThrow(new JwtException("invalid"));

            // when & then
            assertThatThrownBy(() -> authService.logout("invalid-token"))
                    .isInstanceOf(JwtException.class);

            verify(tokenBlacklist, never()).add(anyString(), any(Instant.class));
        }
    }

    // 요청 DTO에 생성자·setter가 없어서 리플렉션으로 필드를 채워요
    private SignupRequest signupRequest() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", EMAIL);
        ReflectionTestUtils.setField(request, "password", RAW_PASSWORD);
        ReflectionTestUtils.setField(request, "name", "홍길동");
        ReflectionTestUtils.setField(request, "nickname", "길동이");
        ReflectionTestUtils.setField(request, "generation", 14);
        ReflectionTestUtils.setField(request, "part", Part.BE);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private User savedUser(Long id) {
        User user = User.create(EMAIL, ENCODED_PASSWORD, "홍길동", "길동이", 14, Part.BE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
