package com.cotato.backend14th.domain.user.controller;

import com.cotato.backend14th.domain.user.dto.MeResponse;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.service.CurrentUserService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final CurrentUserService currentUserService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/api/users/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        User user = currentUserService.requireUser(jwt);
        return ResponseUtils.ok(new MeResponse(user.getId(), user.getEmail(), user.getName(),
                user.getNickname(), user.getGeneration(), user.getPart()));
    }
}
