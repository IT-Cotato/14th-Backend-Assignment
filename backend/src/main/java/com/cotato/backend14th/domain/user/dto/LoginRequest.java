package com.cotato.backend14th.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
    @Email
    private String email;

    @Schema(example = "password1234")
    @NotBlank
    private String password;
}
