package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 모집글·댓글의 작성자 정보예요. 이메일 같은 개인 정보는 담지 않아요.
@Getter
@AllArgsConstructor
public class AuthorResponse {

    private final String nickname;
    private final int generation;
    private final Part part;

    public static AuthorResponse from(User user) {
        return new AuthorResponse(user.getNickname(), user.getGeneration(), user.getPart());
    }
}
