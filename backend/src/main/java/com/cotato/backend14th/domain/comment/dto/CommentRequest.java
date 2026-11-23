package com.cotato.backend14th.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 댓글 작성(10번), 수정(12번)은 검증이 같아서 하나로 써요
@Getter
@NoArgsConstructor
public class CommentRequest {

    @Schema(description = "댓글(신청) 내용 (1~500자, 공백만 불가)", example = "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요.")
    @NotBlank
    @Size(max = 500)
    private String content;
}
