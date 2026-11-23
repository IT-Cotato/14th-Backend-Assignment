package com.cotato.backend14th.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.user.dto.UserResponse;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        // 토큰은 유효하지만 유저가 사라진 경우(DB 초기화 등)도 인증 실패로 봐요
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));
    }
}
