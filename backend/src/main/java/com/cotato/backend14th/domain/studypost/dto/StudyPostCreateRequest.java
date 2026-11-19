package com.cotato.backend14th.domain.studypost.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyPostCreateRequest(
        @Schema(example = "알고리즘 스터디 모집")
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 50, message = "제목은 50자 이하로 입력해 주세요.")
        String title,

        @Schema(example = "주 2회 백준 풀이...")
        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(max = 2000, message = "내용은 2000자 이하로 입력해 주세요.")
        String content
) {
}
