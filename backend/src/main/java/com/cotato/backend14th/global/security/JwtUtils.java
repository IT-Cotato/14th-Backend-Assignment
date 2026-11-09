package com.cotato.backend14th.global.security;

import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.experimental.UtilityClass;

import org.springframework.security.oauth2.jwt.Jwt;

// JWT의 subject(sub)에 담긴 사용자 ID를 꺼내는 도우미예요.
@UtilityClass
public class JwtUtils {

    // 로그인이 필요한 API에서 사용해요. 꺼낼 수 없으면 401이에요.
    public Long getUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new Backend14thException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new Backend14thException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 공개 API에서 사용해요. 비로그인이면 null이에요.
    public Long getUserIdOrNull(Jwt jwt) {
        return jwt == null ? null : getUserId(jwt);
    }
}
