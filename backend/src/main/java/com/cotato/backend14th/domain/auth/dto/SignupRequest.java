
package com.cotato.backend14th.domain.auth.dto;

import com.cotato.backend14th.domain.user.entity.Part;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 10,
                message = "닉네임은 2자 이상 10자 이하여야 합니다.")
        String nickname,

        @NotNull(message = "기수는 필수입니다.")
        @Min(value = 1, message = "기수는 1 이상이어야 합니다.")
        Integer generation,

        @NotNull(message = "파트는 필수입니다.")
        Part part
) {
}
