package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 모집글, 댓글 응답에 들어가는 작성자 정보예요
@Getter
@AllArgsConstructor
public class AuthorResponse {

    @Schema(description = "닉네임", example = "길동이")
    private String nickname;

    @Schema(description = "기수", example = "14")
    private Integer generation;

    @Schema(description = "파트", example = "BE")
    private Part part;

    public static AuthorResponse from(User user) {
        return new AuthorResponse(user.getNickname(), user.getGeneration(), user.getPart());
    }
}
