// ErrorResponse.java
package com.cotato.backend14th.global.common.response;

import java.time.LocalDateTime;

import com.cotato.backend14th.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;

@Getter
public class ErrorResponse {

    private final boolean success = false;
    private final int status;
    private final String code;
    private final String message;
    private final String method;
    private final String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private final LocalDateTime timestamp = LocalDateTime.now();

    private ErrorResponse(int status, String code, String message, String method, String path) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.method = method;
        this.path = path;
    }

    public static ErrorResponse of(ErrorCode errorCode, HttpServletRequest request) {
        return of(errorCode, errorCode.getMessage(), request);
    }

    // 검증 실패처럼 필드별 메시지를 내려줘야 할 때 써요. code와 status는 ErrorCode를 그대로 따라요
    public static ErrorResponse of(ErrorCode errorCode, String message, HttpServletRequest request) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                message,
                request.getMethod(),
                request.getRequestURI()
        );
    }
}