package com.cotato.backend14th.domain.user.controller;

import com.cotato.backend14th.domain.user.dto.AuthDtos.*;
import com.cotato.backend14th.domain.user.service.AuthService;
import com.cotato.backend14th.global.common.response.*;
import com.cotato.backend14th.global.security.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final TokenService tokens;

    @PostMapping("/auth/signup")
    public ResponseEntity<ApiResponse<UserId>> signup(@Valid @RequestBody Signup request) {
        return ResponseUtils.created(auth.signup(request));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<Token>> login(@RequestBody Login request) {
        return ResponseUtils.ok(auth.login(request));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        tokens.revoke(jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<Profile>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseUtils.ok(auth.profile(Long.parseLong(jwt.getSubject())));
    }
}
