package com.cotato.backend14th.global.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.cotato.backend14th.global.common.response.ErrorResponse;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 이번 단계에선 실제로 403을 내릴 권한 규칙이 없지만, PRD 5장이 403도 ErrorResponse로 나가도록 요구해서 구성해둬요.
// 컨텍스트에 등록된 ObjectMapper 빈이 없어서(스프링 기본 컨버터가 내부적으로 자체 생성) 직접 만들어 써요.
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.COMMON_FORBIDDEN, request);

        response.setStatus(ErrorCode.COMMON_FORBIDDEN.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(errorResponse));
    }
}
