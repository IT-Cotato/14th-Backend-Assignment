package com.cotato.backend14th.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 댓글은 내용 하나뿐이라 수정할 때도 content가 필요해요.
public record CommentUpdateRequest(

        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = 500, message = "댓글은 500자 이하여야 합니다.")
        String content

) {
}
