package com.cotato.backend14th.domain.user.dto.response;

import com.cotato.backend14th.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthorResponse {
    private String nickname;
    private int generation;
    private String part;

    public static AuthorResponse from(User user) {
        return new AuthorResponse(user.getNickname(), user.getGeneration(), user.getPart().name());
    }
}
