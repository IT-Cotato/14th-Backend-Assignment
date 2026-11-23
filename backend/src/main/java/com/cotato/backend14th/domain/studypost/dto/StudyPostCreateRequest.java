package com.cotato.backend14th.domain.studypost.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 검증 규칙은 docs/api-spec.md 5번을 따라요.
public record StudyPostCreateRequest(
        @NotBlank @Size(max = 50) String title,
        @NotBlank @Size(max = 2000) String content
) {
}
