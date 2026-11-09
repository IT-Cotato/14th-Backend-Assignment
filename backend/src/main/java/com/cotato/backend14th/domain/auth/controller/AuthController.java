
package com.cotato.backend14th.domain.auth.controller;

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.auth.service.AuthService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import com.cotato.backend14th.global.config.SwaggerConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 API", description = "회원가입 및 로그인 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @Operation(
            summary = "회원가입",
            description = "이메일, 비밀번호, 이름, 닉네임, 기수, 파트를 입력해 회원가입합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);

        return ResponseUtils.created(response);
    }

    // 로그인
    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고 JWT를 발급받습니다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseUtils.ok(response);
    }

    // 로그아웃
    @Operation(
            summary = "로그아웃",
            description = "현재 JWT를 만료 시각까지 무효화해요. 로그아웃한 토큰으로 요청하면 401을 반환해요.",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt
    ) {
        authService.logout(jwt);

        return ResponseEntity.noContent().build();
    }
}
