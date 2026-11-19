package com.cotato.backend14th.domain.auth.dto;

import com.cotato.backend14th.domain.user.entity.Part;

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

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Size(min = 2, max = 10)
    private String nickname;

    @NotNull
    @Min(1)
    private Integer generation;

    // 목록에 없는 값은 JSON 변환 단계에서 실패해서 400 INVALID_INPUT이 돼요
    @NotNull
    private Part part;
}
