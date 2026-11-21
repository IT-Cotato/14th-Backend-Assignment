// AuthorResponse.java
package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 모집글과 댓글 응답에 함께 쓰는 작성자 정보예요
@Getter
@AllArgsConstructor
public class AuthorResponse {

    private final String nickname;
    private final Integer generation;
    private final Part part;

    public static AuthorResponse from(User user) {
        return new AuthorResponse(user.getNickname(), user.getGeneration(), user.getPart());
    }
}
