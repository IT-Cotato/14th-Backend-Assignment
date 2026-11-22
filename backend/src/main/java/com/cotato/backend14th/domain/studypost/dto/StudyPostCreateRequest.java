package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.global.common.request.TextDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyPostCreateRequest(
        @JsonDeserialize(using = TextDeserializer.class) @NotBlank @Size(max = 50) String title,
        @JsonDeserialize(using = TextDeserializer.class) @NotBlank @Size(max = 2000) String content
) {
}
