package com.cotato.backend14th.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
// 빈 생성자를 사용해야 하는 이유 : 요청 JSON을 객체로 바꾸는 라이브러리가 빈 객체를 먼저 만들고 값을 채우는 방식으로 사용
@NoArgsConstructor
public class LoginRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

}
