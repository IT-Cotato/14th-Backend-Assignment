package com.cotato.backend14th.global.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 인증이 필요한 API를 인증 없이 호출하면 실행돼요.
// 필터 단계의 예외는 @RestControllerAdvice가 잡지 못해서,
// HandlerExceptionResolver로 넘겨 GlobalExceptionHandler가 ErrorResponse로 만들게 해요.
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.resolver = resolver;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException
    ) {
        resolver.resolveException(request, response, null, new Backend14thException(ErrorCode.UNAUTHORIZED));
    }
}
