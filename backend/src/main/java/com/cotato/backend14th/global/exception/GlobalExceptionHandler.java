// GlobalExceptionHandler.java
package com.cotato.backend14th.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cotato.backend14th.global.common.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 우리가 직접 던진 Backend14thException을 처리해요
    @ExceptionHandler(Backend14thException.class)
    public ResponseEntity<ErrorResponse> handleBackend14thException(
            Backend14thException e, HttpServletRequest request
    ) {
        log.warn("[Backend14thException] {} {} | code={} | message={}",
                request.getMethod(), request.getRequestURI(),
                e.getErrorCode().getCode(), e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e.getErrorCode(), request));
    }

    // @Valid 검증에 실패하면 400으로 처리해요
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation (MethodArgumentNotValidException e, HttpServletRequest request)
    {
        log.warn("[ValidationException] {} {} | {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, request));
    }

    // JSON 형식이 틀리거나 enum 값이 올바르지 않을 때도 400으로 처리해요
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request
    ) {
        // 서버에 띄우는 로그
        log.warn("[NotReadable] {} {} | {}", request.getMethod(), request.getRequestURI(), e.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, request));
    }

    // 그 외 예상치 못한 모든 예외는 500으로 처리해요
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request
    ) {
        log.error("[UnhandledException] {} {} | {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        return ResponseEntity
                .status(ErrorCode.COMMON_INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_INTERNAL_SERVER_ERROR, request));
    }
}