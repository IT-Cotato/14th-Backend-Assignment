package com.cotato.backend14th.global.security;

import com.cotato.backend14th.domain.auth.service.TokenRevocationService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

// JWT 검증 단계에서 로그아웃으로 무효화된 토큰인지 확인해요.
// 실패하면 Security가 401로 처리하고 JwtAuthenticationEntryPoint가 ErrorResponse를 내려줘요.
@Component
@RequiredArgsConstructor
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED_TOKEN_ERROR = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN,
            "로그아웃된 토큰입니다.",
            null
    );

    private final TokenRevocationService tokenRevocationService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (tokenRevocationService.isRevoked(jwt.getTokenValue())) {
            return OAuth2TokenValidatorResult.failure(REVOKED_TOKEN_ERROR);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
