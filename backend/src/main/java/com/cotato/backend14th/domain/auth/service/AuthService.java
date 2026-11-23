package com.cotato.backend14th.domain.auth.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.LoginResponse;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.auth.dto.SignupResponse;
import com.cotato.backend14th.domain.auth.entity.RevokedToken;
import com.cotato.backend14th.domain.auth.repository.RevokedTokenRepository;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtProvider;
import com.cotato.backend14th.global.security.JwtProvider.ParsedToken;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new Backend14thException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.create(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getNickname(),
                request.getGeneration(),
                request.getPart()
        );

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // existsByEmail 확인과 저장 사이에 같은 이메일로 동시에 가입한 경우 DB의 UK가 막아줘요
            throw new Backend14thException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        return new SignupResponse(user.getId());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 이메일이 없는 경우와 비밀번호가 틀린 경우를 구분하지 않고 같은 에러를 내려줘요
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new Backend14thException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new Backend14thException(ErrorCode.INVALID_CREDENTIALS);
        }
        return new LoginResponse(jwtProvider.createToken(user.getId()), TOKEN_TYPE);
    }

    @Transactional
    public void logout(String token) {
        ParsedToken parsed = jwtProvider.parse(token);

        revokedTokenRepository.deleteExpired(LocalDateTime.now());
        if (!revokedTokenRepository.existsByJti(parsed.jti())) {
            revokedTokenRepository.save(RevokedToken.of(parsed.jti(), parsed.expiresAt()));
        }
    }
}
