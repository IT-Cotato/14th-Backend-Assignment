package com.cotato.backend14th.domain.user.controller;

import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.RevokedTokens;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LogoutController {

    private final RevokedTokens revokedTokens;

    @Operation(summary = "로그아웃")
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || !revokedTokens.revoke(jwt)) {
            throw new Backend14thException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.noContent().build();
    }
}
