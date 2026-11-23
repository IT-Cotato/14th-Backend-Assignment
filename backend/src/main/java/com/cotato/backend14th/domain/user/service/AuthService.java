package com.cotato.backend14th.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.user.dto.LoginRequest;
import com.cotato.backend14th.domain.user.dto.LoginResponse;
import com.cotato.backend14th.domain.user.dto.SignupRequest;
import com.cotato.backend14th.domain.user.dto.SignupResponse;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtProvider;
import com.cotato.backend14th.global.security.TokenBlacklist;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new Backend14thException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .generation(request.getGeneration())
                .part(request.getPart())
                .build();

        return new SignupResponse(userRepository.save(user).getId());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 이메일이 없는 경우와 비밀번호가 틀린 경우를 같은 에러로 내려서 가입 여부가 드러나지 않게 해요
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new Backend14thException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new Backend14thException(ErrorCode.INVALID_CREDENTIALS);
        }

        return LoginResponse.bearer(jwtProvider.createAccessToken(user.getId()));
    }

    public void logout(String accessToken) {
        // 필터를 통과한 유효한 토큰만 여기까지 와요. 만료 시각까지만 막으면 그 뒤엔 어차피 만료로 거부돼요
        tokenBlacklist.add(accessToken, jwtProvider.getExpiration(accessToken));
    }
}
