// AuthService.java
package com.cotato.backend14th.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.auth.dto.TokenResponse;
import com.cotato.backend14th.domain.auth.entity.LogoutToken;
import com.cotato.backend14th.domain.auth.repository.LogoutTokenRepository;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final LogoutTokenRepository logoutTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new Backend14thException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = userRepository.save(User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .generation(request.getGeneration())
                .part(request.getPart())
                .build());

        return new SignupResponse(user.getId());
    }

    // 이메일이 없는 경우와 비밀번호가 틀린 경우를 구분하지 않고 같은 에러를 내려줘요
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .filter(found -> passwordEncoder.matches(request.getPassword(), found.getPassword()))
                .orElseThrow(() -> new Backend14thException(ErrorCode.INVALID_CREDENTIALS));

        return TokenResponse.bearer(jwtProvider.createAccessToken(user.getId()));
    }

    // 사용한 토큰을 만료 시각과 함께 저장해서, 같은 토큰으로 다시 요청하면 401이 되게 해요
    @Transactional
    public void logout(String token) {
        if (logoutTokenRepository.existsByToken(token)) {
            return;
        }
        jwtProvider.getExpiresAt(token)
                .ifPresent(expiresAt -> logoutTokenRepository.save(new LogoutToken(token, expiresAt)));
    }
}
