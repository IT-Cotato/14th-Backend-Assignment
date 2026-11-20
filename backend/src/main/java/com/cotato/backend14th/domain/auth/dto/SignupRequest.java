// SignupRequest.java
package com.cotato.backend14th.domain.auth.dto;

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
    @NotBlank(message = "이메일 형식이 올바르지 않습니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @Schema(example = "password1234")
    @NotNull(message = "비밀번호는 8자 이상이어야 합니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @Schema(example = "홍길동")
    @NotBlank(message = "이름을 입력해야 합니다.")
    private String name;

    @Schema(example = "길동이")
    @NotNull(message = "닉네임은 2자 이상 10자 이하여야 합니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
    private String nickname;

    @Schema(example = "14")
    @NotNull(message = "기수는 1 이상의 정수여야 합니다.")
    @Min(value = 1, message = "기수는 1 이상의 정수여야 합니다.")
    private Integer generation;

    @Schema(example = "BE")
    @NotNull(message = "파트는 PM, DE, BE, FE 중 하나여야 합니다.")
    private Part part;
}
