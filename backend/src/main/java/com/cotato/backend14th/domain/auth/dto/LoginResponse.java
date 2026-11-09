
package com.cotato.backend14th.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
}
