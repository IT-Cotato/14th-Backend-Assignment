package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

public record MyInfoResponse(
        Long userId,
        String email,
        String name,
        String nickname,
        Integer generation,
        Part part
) {

    public static MyInfoResponse from(User user) {
        return new MyInfoResponse(user.getId(), user.getEmail(), user.getName(),
                user.getNickname(), user.getGeneration(), user.getPart());
    }
}
