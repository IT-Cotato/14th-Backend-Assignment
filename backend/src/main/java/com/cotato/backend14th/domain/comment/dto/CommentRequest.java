package com.cotato.backend14th.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 댓글 작성과 수정에 함께 써요. 검증 규칙은 docs/api-spec.md 10번(content 1~500자)을 따라요.
public record CommentRequest(
        @NotBlank @Size(max = 500) String content
) {
}
