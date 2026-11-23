package com.cotato.backend14th.domain.auth.service;

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

// 회원가입·로그인 비즈니스 로직을 확인해요.
// 비밀번호 암호화와 JWT 발급은 Mock 대신 실제 BCrypt와 Nimbus 인코더로 검증해요.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final SecretKey KEY = new SecretKeySpec(
            "test-only-jwt-secret-key-at-least-32-bytes!!".getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
    );

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRevocationService tokenRevocationService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                new NimbusJwtEncoder(new ImmutableSecret<>(KEY)),
                tokenRevocationService
        );
    }

    @Test
    @DisplayName("회원가입하면 비밀번호를 BCrypt로 암호화해서 저장하고 userId를 반환해요")
    void signup_encodesPassword() {
        given(userRepository.existsByEmail("user@gmail.com")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        SignupResponse response = authService.signup(signupRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(captor.capture());
        User saved = captor.getValue();

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(saved.getPassword()).isNotEqualTo("password1234").startsWith("$2");
        assertThat(passwordEncoder.matches("password1234", saved.getPassword())).isTrue();
        assertThat(saved.getPart()).isEqualTo(Part.BE);
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 409 EMAIL_ALREADY_EXISTS이고 저장하지 않아요")
    void signup_duplicateEmail() {
        given(userRepository.existsByEmail("user@gmail.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(signupRequest()))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("로그인하면 sub가 회원 ID이고 1시간 뒤 만료되는 HS256 JWT를 발급해요")
    void login_issuesJwt() {
        given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(savedUser()));

        LoginResponse response = authService.login(new LoginRequest("user@gmail.com", "password1234"));

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();
        Jwt jwt = decoder.decode(response.accessToken());

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("user@gmail.com");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("같은 초에 연달아 로그인해도 jti가 달라 매번 다른 토큰을 발급해요 (로그아웃 직후 재로그인 401 방지)")
    void login_issuesUniqueTokenEvenInSameSecond() {
        given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(savedUser()));
        LoginRequest request = new LoginRequest("user@gmail.com", "password1234");

        String first = authService.login(request).accessToken();
        String second = authService.login(request).accessToken();

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();
        assertThat(second).isNotEqualTo(first);
        assertThat(decoder.decode(first).getId()).isNotBlank()
                .isNotEqualTo(decoder.decode(second).getId());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 INVALID_CREDENTIALS예요")
    void login_wrongPassword() {
        given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(savedUser()));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@gmail.com", "wrong-password")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("없는 이메일도 같은 401 INVALID_CREDENTIALS예요 (가입 여부를 노출하지 않아요)")
    void login_unknownEmail() {
        given(userRepository.findByEmail("none@gmail.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("none@gmail.com", "password1234")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    private SignupRequest signupRequest() {
        return new SignupRequest("user@gmail.com", "password1234", "홍길동", "길동이", 14, Part.BE);
    }

    private User savedUser() {
        User user = new User(
                "user@gmail.com", passwordEncoder.encode("password1234"), "홍길동", "길동이", 14, Part.BE
        );
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }
}
