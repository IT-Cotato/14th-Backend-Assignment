package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 바꿀 필드만 보내요. 보내지 않은 필드(null)는 그대로 둬요.
// @Size는 null을 통과시키고, 보낸 값만 길이를 검사해요.
@Getter
@NoArgsConstructor
public class StudyPostUpdateRequest {

    @Size(min = 1, max = 50)
    private String title;

    @Size(min = 1, max = 2000)
    private String content;

    // 목록에 없는 값은 JSON 변환 단계에서 실패해서 400 INVALID_INPUT이 돼요
    private StudyStatus studyStatus;
}
