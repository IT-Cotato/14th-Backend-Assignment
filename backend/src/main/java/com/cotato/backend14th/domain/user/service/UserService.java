package com.cotato.backend14th.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.user.dto.MyInfoResponse;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public MyInfoResponse getMyInfo(Long userId) {
        // 토큰은 유효하지만 사용자가 없는 경우도 인증 실패로 봐요
        return userRepository.findById(userId)
                .map(MyInfoResponse::from)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));
    }
}
