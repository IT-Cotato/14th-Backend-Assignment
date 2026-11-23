package com.cotato.backend14th.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new Backend14thException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .nickname(request.nickname().trim())
                .generation(request.generation())
                .part(request.part())
                .build();
        return new SignupResponse(userRepository.save(user).getId());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new Backend14thException(ErrorCode.INVALID_CREDENTIALS));
        return LoginResponse.bearer(jwtTokenProvider.createAccessToken(user.getId()));
    }

    @Transactional
    public void logout(String token) {
        var tokenInfo = jwtTokenProvider.parse(token)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));
        tokenBlacklistService.blacklist(token, tokenInfo.expiresAt());
    }
}
