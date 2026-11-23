package com.cotato.backend14th.domain.studypost.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class StudyPostUpdateRequest {

    @Size(min = 1, max = 50)
    private String title;

    @Size(min = 1, max = 2000)
    private String content;

    // 컨트롤러에서 바로 enum으로 받으면 잘못된 값이 역직렬화 단계에서 500으로 떨어지므로
    // String + 패턴 검증으로 받고 서비스에서 StudyStatus.valueOf()로 변환해요.
    @Pattern(regexp = "RECRUITING|CLOSED")
    private String studyStatus;
}
