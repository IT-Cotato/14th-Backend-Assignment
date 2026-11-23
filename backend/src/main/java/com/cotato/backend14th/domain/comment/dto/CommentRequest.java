package com.cotato.backend14th.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 댓글 작성·수정 공용. 두 API의 본문과 검증이 같아요.
@Getter
@NoArgsConstructor
public class CommentRequest {

    @NotBlank
    @Size(max = 500)
    private String content;
}
