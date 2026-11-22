package com.cotato.backend14th.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.user.dto.UserResponse;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("내 정보를 조회하면 기수와 파트를 포함해 돌려줘요")
    void getMe_success() {
        User user = User.builder()
                .email("user@gmail.com")
                .password("$2a$10$encoded")
                .name("홍길동")
                .nickname("길동이")
                .generation(14)
                .part(Part.BE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.getMe(1L);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("user@gmail.com");
        assertThat(response.getName()).isEqualTo("홍길동");
        assertThat(response.getNickname()).isEqualTo("길동이");
        assertThat(response.getGeneration()).isEqualTo(14);
        assertThat(response.getPart()).isEqualTo(Part.BE);
    }

    @Test
    @DisplayName("응답 DTO에는 비밀번호 필드가 없어요")
    void userResponse_hasNoPasswordField() {
        assertThat(Arrays.stream(UserResponse.class.getDeclaredFields()).map(f -> f.getName()))
                .doesNotContain("password");
    }

    @Test
    @DisplayName("토큰의 userId에 해당하는 유저가 없으면 UNAUTHORIZED를 던져요")
    void getMe_userNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(99L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
