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

    // 토큰은 유효한데 회원이 없으면(탈퇴 등) 인증 실패로 봐요
    @Transactional(readOnly = true)
    public UserResponse getMyInfo(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));
    }
}
