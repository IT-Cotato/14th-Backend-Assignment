package com.cotato.backend14th.domain.study;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCommentRequest {
    @NotBlank
    @Size(min = 1, max = 500)
    private String content;
}
