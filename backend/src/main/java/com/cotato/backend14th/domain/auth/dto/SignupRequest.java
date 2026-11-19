package com.cotato.backend14th.domain.auth.dto;

import com.cotato.backend14th.domain.user.entity.Part;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Schema(example = "user@gmail.com")
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않아요.")
        @Size(max = 100, message = "이메일은 100자 이하로 입력해 주세요.")
        String email,

        @Schema(example = "password1234")
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해 주세요.")
        String password,

        @Schema(example = "홍길동")
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 20, message = "이름은 20자 이하로 입력해 주세요.")
        String name,

        @Schema(example = "길동이")
        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해 주세요.")
        String nickname,

        @Schema(example = "14")
        @NotNull(message = "기수를 입력해 주세요.")
        @Min(value = 1, message = "기수는 1 이상의 정수로 입력해 주세요.")
        Integer generation,

        @Schema(example = "BE", description = "PM, DE, BE, FE 중 하나")
        @NotNull(message = "파트를 선택해 주세요.")
        Part part
) {
}
