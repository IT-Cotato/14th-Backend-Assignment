package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 바꿀 필드만 보내요. null이면 그대로 두고, 값이 있으면 작성할 때와 같은 규칙으로 검증해요.
// @Pattern은 공백만 있는 값을 막아요. (null은 통과해요)
public record StudyPostUpdateRequest(
        @Size(max = 50) @Pattern(regexp = "(?s).*\\S.*") String title,
        @Size(max = 2000) @Pattern(regexp = "(?s).*\\S.*") String content,
        StudyStatus studyStatus
) {
}
