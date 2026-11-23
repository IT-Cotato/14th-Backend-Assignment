package com.cotato.backend14th.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 댓글 작성과 수정에 함께 쓴다
@Getter
@NoArgsConstructor
public class CommentRequest {

    @NotBlank @Size(max = 500)
    private String content;
}
