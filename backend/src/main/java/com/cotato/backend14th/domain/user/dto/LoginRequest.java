package com.cotato.backend14th.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRequest(
        String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password
) {
}
