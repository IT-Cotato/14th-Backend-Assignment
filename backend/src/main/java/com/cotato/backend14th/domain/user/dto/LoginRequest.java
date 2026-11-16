package com.cotato.backend14th.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @Schema(description = "이메일", example = "user@gmail.com")
    @NotBlank
    private String email;

    @Schema(description = "비밀번호", example = "password1234")
    @NotBlank
    private String password;
}
