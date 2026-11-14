package com.cotato.backend14th.domain.auth.dto;

import com.cotato.backend14th.domain.user.entity.Part;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 검증 규칙은 docs/api-spec.md 1번 회원가입을 따라요. name은 명세에 규칙이 없어 필수 값만 확인해요.
public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String name,
        @NotBlank @Size(min = 2, max = 10) String nickname,
        @NotNull @Min(1) Integer generation,
        @NotNull Part part
) {
}
