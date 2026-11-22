package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 모집글·댓글의 작성자 정보 (api-spec "작성자 정보" 형식)
@Getter
@AllArgsConstructor
public class AuthorResponse {

    private String nickname;
    private Integer generation;
    private Part part;

    public static AuthorResponse from(User user) {
        return new AuthorResponse(user.getNickname(), user.getGeneration(), user.getPart());
    }
}
