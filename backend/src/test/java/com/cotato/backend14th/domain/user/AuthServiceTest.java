package com.cotato.backend14th.domain.user;

import com.cotato.backend14th.domain.user.dto.AuthDtos.Signup;
import com.cotato.backend14th.domain.user.entity.*;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.domain.user.service.AuthService;
import com.cotato.backend14th.global.security.TokenService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Test
    void storesNormalizedEmailAndBcryptHash() {
        var users = mock(UserRepository.class);
        var encoder = new BCryptPasswordEncoder();
        var service = new AuthService(users, encoder, mock(TokenService.class));
        User saved = mock(User.class);
        when(saved.getId()).thenReturn(1L);
        when(users.saveAndFlush(any(User.class))).thenReturn(saved);
        String password = UUID.randomUUID().toString();
        assertThat(service.signup(new Signup("MEMBER@example.com", password, "name", "nickname", 14, Part.BE)).userId())
                .isEqualTo(1L);
        var user = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(user.capture());
        assertThat(user.getValue().getEmail()).isEqualTo("member@example.com");
        assertThat(user.getValue().getPassword()).isNotEqualTo(password);
        assertThat(encoder.matches(password, user.getValue().getPassword())).isTrue();
    }
}
