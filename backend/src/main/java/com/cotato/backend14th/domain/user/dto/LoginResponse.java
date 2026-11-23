package com.cotato.backend14th.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class LoginResponse {

    private static final String BEARER = "Bearer";

    @Schema(description = "액세스 토큰", example = "eyJhbGciOi...")
    private final String accessToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private final String tokenType;

    public LoginResponse(String accessToken) {
        this.accessToken = accessToken;
        this.tokenType = BEARER;
    }
}
