package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

// 비밀번호는 절대 응답에 넣지 않아요
public record MyInfoResponse(
        Long userId,
        String email,
        String name,
        String nickname,
        Integer generation,
        Part part
) {
    public static MyInfoResponse from(User user) {
        return new MyInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getGeneration(),
                user.getPart()
        );
    }
}
