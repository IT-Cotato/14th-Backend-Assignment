package com.cotato.backend14th.domain.study.dto;

import com.cotato.backend14th.domain.study.entity.StudyStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 바꿀 필드만 보내요. null(안 보냄)은 검증을 통과하고 변경하지 않아요
// 보냈는데 빈 값이거나 공백만 있으면 400이에요
@Getter
@NoArgsConstructor
public class StudyPostUpdateRequest {

    private static final String NOT_BLANK_REGEX = "(?s).*\\S.*";

    @Schema(description = "제목 (보내면 1~50자, 공백만 불가)", example = "수정된 제목")
    @Size(min = 1, max = 50)
    @Pattern(regexp = NOT_BLANK_REGEX)
    private String title;

    @Schema(description = "내용 (보내면 1~2000자, 공백만 불가)", example = "수정된 내용")
    @Size(min = 1, max = 2000)
    @Pattern(regexp = NOT_BLANK_REGEX)
    private String content;

    @Schema(description = "모집 상태", example = "CLOSED")
    private StudyStatus studyStatus;
}
