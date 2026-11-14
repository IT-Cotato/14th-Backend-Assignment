package com.cotato.backend14th.domain.auth.service;

import java.time.Instant;

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
import com.cotato.backend14th.global.security.TokenBlacklist;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new Backend14thException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .nickname(request.nickname())
                .generation(request.generation())
                .part(request.part())
                .build();

        return new SignupResponse(userRepository.save(user).getId());
    }

    // 이메일이 없을 때와 비밀번호가 틀릴 때 같은 에러를 내려서, 가입 여부를 알려주지 않아요
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new Backend14thException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new Backend14thException(ErrorCode.INVALID_CREDENTIALS);
        }

        return LoginResponse.bearer(jwtTokenProvider.createAccessToken(user.getId()));
    }

    // 사용한 토큰을 만료 시각까지 무효 처리해요
    public void logout(String accessToken) {
        Instant expiration = jwtTokenProvider.getExpiration(accessToken)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));
        tokenBlacklist.add(accessToken, expiration);
    }
}
