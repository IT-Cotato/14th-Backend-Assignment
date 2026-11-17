package com.cotato.backend14th.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SignupRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    @Size(min = 2, max = 10)
    private String nickname;

    @NotNull
    @Min(1)
    private Integer generation;

    // 컨트롤러에서 바로 enum으로 받으면 잘못된 값이 역직렬화 단계에서 500으로 떨어지므로
    // String + 패턴 검증으로 받고 서비스에서 Part.valueOf()로 변환해요.
    @NotBlank
    @Pattern(regexp = "PM|DE|BE|FE")
    private String part;
}
