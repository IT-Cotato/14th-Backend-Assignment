package com.cotato.backend14th.domain.study;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateStudyPostRequest {
    @Size(min = 1, max = 50)
    private String title;
    @Size(min = 1, max = 2000)
    private String content;
    private StudyStatus studyStatus;
}