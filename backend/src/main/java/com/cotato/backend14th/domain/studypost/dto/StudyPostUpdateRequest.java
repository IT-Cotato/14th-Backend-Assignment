// StudyPostUpdateRequest.java
package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 바꿀 필드만 보내요. 보내지 않은(null) 필드는 검증하지 않고 그대로 둬요
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPostUpdateRequest {

    @Schema(example = "수정된 제목")
    @Size(max = 50, message = "제목은 1자 이상 50자 이하여야 합니다.")
    @Pattern(regexp = "(?s).*\\S.*", message = "제목은 1자 이상 50자 이하여야 합니다.")
    private String title;

    @Schema(example = "수정된 내용")
    @Size(max = 2000, message = "내용은 1자 이상 2000자 이하여야 합니다.")
    @Pattern(regexp = "(?s).*\\S.*", message = "내용은 1자 이상 2000자 이하여야 합니다.")
    private String content;

    @Schema(example = "CLOSED")
    private StudyStatus studyStatus;
}
