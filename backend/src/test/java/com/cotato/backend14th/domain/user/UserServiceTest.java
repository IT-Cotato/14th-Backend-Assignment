package com.cotato.backend14th.domain.user;

import static com.cotato.backend14th.support.Fixtures.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cotato.backend14th.domain.user.dto.UserResponse;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.domain.user.service.UserService;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.support.Fixtures;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @InjectMocks
    UserService service;

    @Test
    @DisplayName("내 정보에는 기수와 파트가 들어 있고 비밀번호 필드는 없다")
    void getMe() {
        User user = Fixtures.user(1L, "길동이", Part.FE);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = service.getMe(1L);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getNickname()).isEqualTo("길동이");
        assertThat(response.getGeneration()).isEqualTo(14);
        assertThat(response.getPart()).isEqualTo(Part.FE);
        assertThat(UserResponse.class.getDeclaredFields()).extracting("name").doesNotContain("password");
    }

    @Test
    @DisplayName("토큰의 사용자가 DB에 없으면 UNAUTHORIZED이다")
    void getMeWithUnknownUser() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertErrorCode(() -> service.getMe(99L), ErrorCode.UNAUTHORIZED);
    }
}
