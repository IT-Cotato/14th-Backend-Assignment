package com.cotato.backend14th.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupResponse {

    @Schema(description = "가입한 회원 ID", example = "1")
    private Long userId;
}
