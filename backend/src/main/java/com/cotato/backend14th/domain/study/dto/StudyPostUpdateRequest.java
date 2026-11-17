package com.cotato.backend14th.domain.study.dto;

import com.cotato.backend14th.domain.study.entity.StudyStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 바꿀 필드만 보내요. 보내지 않은(null) 필드는 그대로 둬요.
// 값을 보냈는데 공백뿐인 경우는 서비스에서 INVALID_INPUT으로 처리해요.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPostUpdateRequest {

    @Schema(example = "수정된 제목", nullable = true)
    @Size(max = 50)
    private String title;

    @Schema(example = "수정된 내용", nullable = true)
    @Size(max = 2000)
    private String content;

    @Schema(example = "CLOSED", nullable = true, description = "RECRUITING 또는 CLOSED")
    private StudyStatus studyStatus;
}
