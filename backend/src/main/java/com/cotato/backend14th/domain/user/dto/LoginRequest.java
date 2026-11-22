package com.cotato.backend14th.domain.user.dto;

import com.cotato.backend14th.global.common.request.TextDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record LoginRequest(
        @JsonDeserialize(using = TextDeserializer.class) String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @JsonDeserialize(using = TextDeserializer.class) String password
) {
}
