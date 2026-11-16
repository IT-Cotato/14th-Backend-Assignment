package com.cotato.backend14th.domain.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudyPostCreateRequest {

    @Schema(description = "제목 (1~50자, 공백만 불가)", example = "알고리즘 스터디 모집")
    @NotBlank
    @Size(max = 50)
    private String title;

    @Schema(description = "내용 (1~2000자, 공백만 불가)", example = "주 2회 백준 풀이...")
    @NotBlank
    @Size(max = 2000)
    private String content;
}
