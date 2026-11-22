package com.cotato.backend14th.domain.studypost.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPostCreateRequest {

    @Schema(example = "알고리즘 스터디 모집")
    @NotBlank
    @Size(max = 50)
    private String title;

    @Schema(example = "주 2회 백준 풀이...")
    @NotBlank
    @Size(max = 2000)
    private String content;
}
