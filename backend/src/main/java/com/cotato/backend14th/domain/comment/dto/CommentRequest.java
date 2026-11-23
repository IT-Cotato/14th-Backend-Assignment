// CommentRequest.java
package com.cotato.backend14th.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 댓글 작성과 수정이 같은 모양이라 함께 써요
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @Schema(example = "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요.")
    @NotBlank(message = "댓글은 1자 이상 500자 이하여야 합니다.")
    @Size(max = 500, message = "댓글은 1자 이상 500자 이하여야 합니다.")
    private String content;
}
