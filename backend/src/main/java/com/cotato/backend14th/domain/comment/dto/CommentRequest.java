package com.cotato.backend14th.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 댓글 작성·수정 공통 */
public record CommentRequest(
        @Schema(example = "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요.")
        @NotBlank(message = "댓글 내용을 입력해 주세요.")
        @Size(max = 500, message = "댓글은 1~500자여야 해요.")
        String content
) {
}
