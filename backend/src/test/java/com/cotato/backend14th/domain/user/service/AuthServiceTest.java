package com.cotato.backend14th.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cotato.backend14th.domain.user.dto.request.LoginRequest;
import com.cotato.backend14th.domain.user.dto.response.LoginResponse;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtTokenProvider;
import com.cotato.backend14th.global.security.TokenBlacklistService;

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

    @Mock
    private LoginRequest loginRequest;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_success() {
        User user = new User(1L, "test@example.com", "encoded-password", "홍길동", "길동이", 14, Part.BE);
        when(loginRequest.getEmail()).thenReturn("test@example.com");
        when(loginRequest.getPassword()).thenReturn("password1234");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1234", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L)).thenReturn("generated-token");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("generated-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_emailNotFound_throwsException() {
        when(loginRequest.getEmail()).thenReturn("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_wrongPassword_throwsException() {
        User user = new User(1L, "test@example.com", "encoded-password", "홍길동", "길동이", 14, Part.BE);
        when(loginRequest.getEmail()).thenReturn("test@example.com");
        when(loginRequest.getPassword()).thenReturn("wrong-password");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void logout_blacklistsToken() {
        Date expiration = Date.from(Instant.now().plusSeconds(3600));
        when(jwtTokenProvider.getExpiration("some-token")).thenReturn(expiration);

        authService.logout("some-token");

        verify(tokenBlacklistService).blacklist(eq("some-token"), any(Instant.class));
    }
}
