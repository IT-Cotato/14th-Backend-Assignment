package com.cotato.backend14th.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.jwt.JwtTokenProvider;
import com.cotato.backend14th.global.jwt.TokenBlacklist;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenBlacklist tokenBlacklist;
    @Mock private SignupRequest signupRequest;
    @Mock private LoginRequest loginRequest;
    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    void signupSuccess() throws Exception {
        when(signupRequest.getEmail()).thenReturn("test@gmail.com");
        when(signupRequest.getPassword()).thenReturn("Password1");
        when(signupRequest.getName()).thenReturn("한다은");
        when(signupRequest.getNickname()).thenReturn("다은");
        when(signupRequest.getGeneration()).thenReturn(14);
        when(signupRequest.getPart()).thenReturn(Part.BE);
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, 1L);
            return user;
        });

        SignupResponse response = authService.signup(signupRequest);

        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("중복 이메일이면 회원가입 실패")
    void signupDuplicateEmail() {
        when(signupRequest.getEmail()).thenReturn("dup@gmail.com");
        when(userRepository.existsByEmail("dup@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("로그인 성공")
    void loginSuccess() throws Exception {
        User user = user(1L, "encoded");
        when(loginRequest.getEmail()).thenReturn("test@gmail.com");
        when(loginRequest.getPassword()).thenReturn("Password1");
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "encoded")).thenReturn(true);
        when(jwtTokenProvider.createToken(1L)).thenReturn("token");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("틀린 비밀번호면 로그인 실패")
    void loginWrongPassword() throws Exception {
        User user = user(1L, "encoded");
        when(loginRequest.getEmail()).thenReturn("test@gmail.com");
        when(loginRequest.getPassword()).thenReturn("wrong");
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("로그아웃하면 토큰을 블랙리스트에 넣는다")
    void logoutAddsToken() {
        when(jwtTokenProvider.getUserId("token")).thenReturn(1L);

        authService.logout("Bearer token");

        verify(tokenBlacklist).add("token");
    }

    private User user(Long id, String password) throws Exception {
        User user = User.builder()
                .email("test@gmail.com")
                .password(password)
                .name("한다은")
                .nickname("다은")
                .generation(14)
                .part(Part.BE)
                .build();
        setId(user, id);
        return user;
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}