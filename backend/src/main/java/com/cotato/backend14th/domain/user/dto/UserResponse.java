package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 내 정보 조회 응답. 비밀번호는 절대 넣지 않아요.
@Getter
@AllArgsConstructor
public class UserResponse {

    private Long userId;
    private String email;
    private String name;
    private String nickname;
    private Integer generation;
    private Part part;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getGeneration(),
                user.getPart()
        );
    }
}
