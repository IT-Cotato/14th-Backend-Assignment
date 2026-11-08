package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

// 모집글·댓글에 함께 내려주는 작성자 정보예요.
public record AuthorResponse(
        String nickname,
        Integer generation,
        Part part
) {

    public static AuthorResponse from(User user) {
        return new AuthorResponse(
                user.getNickname(),
                user.getGeneration(),
                user.getPart()
        );
    }
}
