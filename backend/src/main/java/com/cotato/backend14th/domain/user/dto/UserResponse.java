package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

// 내 정보 조회 응답이에요. 비밀번호는 포함하지 않아요.
public record UserResponse(
        Long userId,
        String email,
        String name,
        String nickname,
        Integer generation,
        Part part
) {

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
