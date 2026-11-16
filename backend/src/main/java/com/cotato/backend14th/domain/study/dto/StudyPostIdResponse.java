package com.cotato.backend14th.domain.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyPostIdResponse {

    @Schema(description = "모집글 ID", example = "10")
    private Long postId;
}
