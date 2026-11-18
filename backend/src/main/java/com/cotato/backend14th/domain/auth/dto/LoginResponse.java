package com.cotato.backend14th.domain.auth.dto;

import lombok.Getter;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";

    public LoginResponse(String accessToken) {
        this.accessToken = accessToken;
    }

}
