package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.domain.user.entity.Part;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @Schema(description = "이메일 (중복 불가)", example = "user@gmail.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "비밀번호 (8자 이상)", example = "password1234")
    @NotBlank
    @Size(min = 8)
    private String password;

    @Schema(description = "이름", example = "홍길동")
    @NotBlank
    private String name;

    @Schema(description = "닉네임 (2~10자, 중복 허용)", example = "길동이")
    @NotBlank
    @Size(min = 2, max = 10)
    private String nickname;

    @Schema(description = "기수 (1 이상)", example = "14")
    @NotNull
    @Min(1)
    private Integer generation;

    @Schema(description = "파트", example = "BE")
    @NotNull
    private Part part;
}
