package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 바꿀 필드만 보내요. 보낸 필드는 비어 있으면 안 돼요. */
public record StudyPostUpdateRequest(
        @Schema(example = "수정된 제목")
        @Size(min = 1, max = 50, message = "제목은 1~50자여야 해요.")
        @Pattern(regexp = "(?s).*\\S.*", message = "제목을 입력해 주세요.")
        String title,

        @Schema(example = "수정된 내용")
        @Size(min = 1, max = 2000, message = "내용은 1~2000자여야 해요.")
        @Pattern(regexp = "(?s).*\\S.*", message = "내용을 입력해 주세요.")
        String content,

        @Schema(example = "CLOSED")
        StudyStatus studyStatus
) {
}
