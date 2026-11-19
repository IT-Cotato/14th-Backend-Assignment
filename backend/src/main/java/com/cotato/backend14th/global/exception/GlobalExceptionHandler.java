// GlobalExceptionHandler.java
package com.cotato.backend14th.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
                .body(ErrorResponse.of(e.getErrorCode(), e.getMessage(), request));
    }

    // @Valid 검증 실패 → 400 INVALID_INPUT (첫 번째 필드 메시지를 내려줘요)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return invalidInput(message, request);
    }

    // JSON 형식 오류, Enum에 없는 값(part, studyStatus) → 400 INVALID_INPUT
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request
    ) {
        return invalidInput(ErrorCode.INVALID_INPUT.getMessage(), request);
    }

    // 쿼리 파라미터 타입 오류 (status=WRONG, page=abc) → 400 INVALID_INPUT
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request
    ) {
        return invalidInput("'" + e.getName() + "' 값이 올바르지 않아요.", request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException e, HttpServletRequest request
    ) {
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e, HttpServletRequest request) {
        return ResponseEntity
                .status(ErrorCode.COMMON_NOT_FOUND.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_NOT_FOUND, request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request
    ) {
        return ResponseEntity
                .status(ErrorCode.COMMON_BAD_REQUEST.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_BAD_REQUEST, request));
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

    private ResponseEntity<ErrorResponse> invalidInput(String message, HttpServletRequest request) {
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message, request));
    }
}
