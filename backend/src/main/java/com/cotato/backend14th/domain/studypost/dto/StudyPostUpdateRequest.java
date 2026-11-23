package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// PATCH 요청이에요. 모든 필드는 선택이고, 전달한 필드만 수정해요.
// 값을 전달했다면 공백만 있는 문자열은 허용하지 않아요.
public record StudyPostUpdateRequest(

        @Size(min = 1, max = 50, message = "제목은 1자 이상 50자 이하여야 합니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "제목은 공백일 수 없습니다.")
        String title,

        @Size(min = 1, max = 2000, message = "내용은 1자 이상 2000자 이하여야 합니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "내용은 공백일 수 없습니다.")
        String content,

        StudyStatus studyStatus

) {
}
