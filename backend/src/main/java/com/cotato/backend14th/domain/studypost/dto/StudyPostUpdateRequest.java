package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 바꿀 필드만 보내요. 보내지 않은(null) 필드는 그대로 둬요.
 */
public record StudyPostUpdateRequest(
        @Schema(example = "수정된 제목")
        @Size(max = 50, message = "제목은 50자 이하로 입력해 주세요.")
        @Pattern(regexp = "(?s).*\\S.*", message = "제목을 입력해 주세요.")
        String title,

        @Schema(example = "수정된 내용")
        @Size(max = 2000, message = "내용은 2000자 이하로 입력해 주세요.")
        @Pattern(regexp = "(?s).*\\S.*", message = "내용을 입력해 주세요.")
        String content,

        @Schema(example = "CLOSED", description = "RECRUITING 또는 CLOSED")
        StudyStatus studyStatus
) {
}
