package com.cotato.backend14th.domain.comment.dto;

import com.cotato.backend14th.global.common.request.TextDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
        @JsonDeserialize(using = TextDeserializer.class) @NotBlank @Size(max = 500) String content
) {
}
