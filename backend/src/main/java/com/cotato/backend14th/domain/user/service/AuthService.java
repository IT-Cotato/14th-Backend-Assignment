package com.cotato.backend14th.domain.user.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cotato.backend14th.domain.user.dto.request.LoginRequest;
import com.cotato.backend14th.domain.user.dto.response.LoginResponse;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtTokenProvider;
import com.cotato.backend14th.global.security.TokenBlacklistService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new Backend14thException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new Backend14thException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.generateToken(user.getId());
        return new LoginResponse(accessToken, TOKEN_TYPE);
    }

    public void logout(String token) {
        Instant expiresAt = jwtTokenProvider.getExpiration(token).toInstant();
        tokenBlacklistService.blacklist(token, expiresAt);
    }
}
