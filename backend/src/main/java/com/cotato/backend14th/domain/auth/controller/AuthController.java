package com.cotato.backend14th.domain.auth.controller;

import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.auth.service.AuthService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 스웨거 문서용 AI 묶음, 이름 붙이는 용도
@Tag(name = "Auth API", description = "인증 관련 API")
// 해당 클래스가 웹 요청을 받는 Controller 임을 알려주는 기능
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 기수, 파트로 가입해요.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request){
        return ResponseUtils.created(authService.signup(request));
    }
}
