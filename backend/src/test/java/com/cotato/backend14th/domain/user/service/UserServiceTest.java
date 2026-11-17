package com.cotato.backend14th.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cotato.backend14th.domain.user.dto.request.SignupRequest;
import com.cotato.backend14th.domain.user.dto.response.MyInfoResponse;
import com.cotato.backend14th.domain.user.dto.response.SignupResponse;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SignupRequest signupRequest;

    @InjectMocks
    private UserService userService;

    @Test
    void signup_success() {
        when(signupRequest.getEmail()).thenReturn("test@example.com");
        when(signupRequest.getPassword()).thenReturn("password1234");
        when(signupRequest.getName()).thenReturn("홍길동");
        when(signupRequest.getNickname()).thenReturn("길동이");
        when(signupRequest.getGeneration()).thenReturn(14);
        when(signupRequest.getPart()).thenReturn("BE");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1234")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenReturn(new User(1L, "test@example.com", "encoded-password", "홍길동", "길동이", 14, Part.BE));

        SignupResponse response = userService.signup(signupRequest);

        assertThat(response.getUserId()).isEqualTo(1L);
        verify(passwordEncoder).encode("password1234");
    }

    @Test
    void signup_duplicateEmail_throwsException() {
        when(signupRequest.getEmail()).thenReturn("test@example.com");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.signup(signupRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void getMyInfo_success() {
        User user = new User(1L, "test@example.com", "encoded-password", "홍길동", "길동이", 14, Part.BE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MyInfoResponse response = userService.getMyInfo(1L);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("길동이");
        assertThat(response.getPart()).isEqualTo("BE");
    }
}
