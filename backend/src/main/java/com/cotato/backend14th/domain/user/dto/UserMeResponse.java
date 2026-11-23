package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 비밀번호는 응답에 포함하지 않아요
@Getter
@AllArgsConstructor
public class UserMeResponse {

    private final Long userId;
    private final String email;
    private final String name;
    private final String nickname;
    private final int generation;
    private final Part part;

    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getGeneration(),
                user.getPart()
        );
    }
}
