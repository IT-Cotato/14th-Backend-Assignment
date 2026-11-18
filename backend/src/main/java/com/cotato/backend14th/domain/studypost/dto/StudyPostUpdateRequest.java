package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudyPostUpdateRequest {

    @Size(min = 1, max = 50)
    private String title;

    @Size(min = 1, max = 2000)
    private String content;

    private StudyStatus studyStatus;
}
