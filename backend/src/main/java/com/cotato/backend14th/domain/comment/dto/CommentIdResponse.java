package com.cotato.backend14th.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentIdResponse {

    @Schema(description = "댓글 ID", example = "5")
    private Long commentId;
}
