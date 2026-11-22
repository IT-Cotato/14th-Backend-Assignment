package com.cotato.backend14th.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cotato.backend14th.domain.user.dto.LoginRequest;
import com.cotato.backend14th.domain.user.dto.LoginResponse;
import com.cotato.backend14th.domain.user.dto.SignupRequest;
import com.cotato.backend14th.domain.user.dto.SignupResponse;
import com.cotato.backend14th.domain.user.service.AuthService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import com.cotato.backend14th.global.security.JwtProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth API", description = "회원가입, 로그인, 로그아웃 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 이름, 닉네임, 기수, 파트로 가입해요.")
    @SecurityRequirements // 공개 API라 Swagger 자물쇠 표시를 빼요
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseUtils.created(authService.signup(request));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 accessToken을 받아요.")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseUtils.ok(authService.login(request));
    }

    @Operation(summary = "로그아웃", description = "사용한 accessToken을 만료 시각까지 무효 처리해요.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // 인증 필터를 통과했으므로 헤더에 유효한 토큰이 있어요
        authService.logout(jwtProvider.resolveToken(request));
        return ResponseEntity.noContent().build(); // 본문 없는 204는 ApiResponse로 감싸지 않아요
    }
}
