package com.cotato.backend14th.domain.user.controller;

import com.cotato.backend14th.domain.user.dto.UserResponse;
import com.cotato.backend14th.domain.user.service.UserService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import com.cotato.backend14th.global.config.SwaggerConfig;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원 API", description = "회원 정보 관련 API예요.")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 정보 조회
    @Operation(
            summary = "내 정보 조회",
            description = "JWT로 로그인한 사용자의 정보를 조회해요. 비밀번호는 포함하지 않아요.",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserResponse response = userService.getMyInfo(extractUserId(jwt));

        return ResponseUtils.ok(response);
    }

    // JWT의 subject에 담긴 사용자 ID를 꺼내요.
    private Long extractUserId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw new Backend14thException(ErrorCode.UNAUTHORIZED);
        }
    }
}
