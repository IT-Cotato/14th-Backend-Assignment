package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

// 모집글과 댓글의 작성자 정보예요. (docs/api-spec.md 3장)
public record AuthorResponse(String nickname, Integer generation, Part part) {

    public static AuthorResponse from(User user) {
        return new AuthorResponse(user.getNickname(), user.getGeneration(), user.getPart());
    }
}
