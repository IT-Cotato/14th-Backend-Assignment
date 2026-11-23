package com.cotato.backend14th.domain.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 작성·수정 응답 { commentId }
@Getter
@AllArgsConstructor
public class CommentIdResponse {

    private Long commentId;
}
