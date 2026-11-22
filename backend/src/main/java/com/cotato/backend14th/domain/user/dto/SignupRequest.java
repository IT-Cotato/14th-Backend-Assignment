package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @Schema(example = "user@gmail.com")
    @NotBlank
    @Email
    private String email;

    @Schema(example = "password1234")
    @NotBlank
    @Size(min = 8)
    private String password;

    @Schema(example = "홍길동")
    @NotBlank
    private String name;

    @Schema(example = "길동이")
    @NotBlank
    @Size(min = 2, max = 10)
    private String nickname;

    @Schema(example = "14")
    @NotNull
    @Min(1)
    private Integer generation;

    @Schema(example = "BE")
    @NotNull
    private Part part;
}
