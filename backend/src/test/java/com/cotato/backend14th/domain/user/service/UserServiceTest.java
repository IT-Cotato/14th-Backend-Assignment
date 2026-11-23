package com.cotato.backend14th.domain.user.service;

import static com.cotato.backend14th.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("내 정보를 조회하면 기수와 파트를 포함한 사용자 정보를 돌려준다")
    void getMyInfo() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L, Part.DE)));

        // when
        UserResponse response = userService.getMyInfo(1L);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("user1@test.com");
        assertThat(response.getName()).isEqualTo("테스트1");
        assertThat(response.getNickname()).isEqualTo("닉네임1");
        assertThat(response.getGeneration()).isEqualTo(14);
        assertThat(response.getPart()).isEqualTo(Part.DE);
    }

    @Test
    @DisplayName("토큰의 사용자가 없으면 UNAUTHORIZED 예외가 난다")
    void getMyInfoWithUnknownUser() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMyInfo(99L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
