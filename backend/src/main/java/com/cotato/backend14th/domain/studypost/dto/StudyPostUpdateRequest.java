package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 바꿀 필드만 보내요. null이면 그대로 두고, 보냈다면 공백만 있는 값은 막아요
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPostUpdateRequest {

    private static final String NOT_BLANK_PATTERN = "(?s).*\\S.*";

    @Schema(example = "수정된 제목")
    @Size(min = 1, max = 50)
    @Pattern(regexp = NOT_BLANK_PATTERN)
    private String title;

    @Schema(example = "수정된 내용")
    @Size(min = 1, max = 2000)
    @Pattern(regexp = NOT_BLANK_PATTERN)
    private String content;

    @Schema(example = "CLOSED")
    private StudyStatus studyStatus;
}
