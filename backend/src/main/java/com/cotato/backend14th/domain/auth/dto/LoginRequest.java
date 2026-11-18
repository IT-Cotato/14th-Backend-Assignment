package com.cotato.backend14th.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(example = "user@gmail.com")
    @NotBlank
    private String email;

    @Schema(example = "password1234")
    @NotBlank
    private String password;
}
