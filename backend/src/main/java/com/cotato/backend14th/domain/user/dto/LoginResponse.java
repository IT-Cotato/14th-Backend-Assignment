package com.cotato.backend14th.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private static final String BEARER = "Bearer";

    private String accessToken;
    private String tokenType;

    public static LoginResponse bearer(String accessToken) {
        return new LoginResponse(accessToken, BEARER);
    }
}
