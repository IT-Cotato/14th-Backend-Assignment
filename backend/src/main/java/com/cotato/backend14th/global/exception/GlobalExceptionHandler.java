// GlobalExceptionHandler.java
package com.cotato.backend14th.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
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

        return toResponse(e.getErrorCode(), e.getMessage(), request);
    }

    // @Valid 검증 실패: 첫 번째 필드 오류 메시지를 그대로 내려줘요
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT, message, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(
            HandlerMethodValidationException e, HttpServletRequest request
    ) {
        String message = e.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT, message, request);
    }

    // JSON 형식 오류, enum에 없는 값(part: "XX"), 숫자 자리에 문자 등
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request
    ) {
        return toResponse(ErrorCode.INVALID_INPUT, "요청 본문의 형식이나 값이 올바르지 않아요.", request);
    }

    // 쿼리 파라미터 타입 오류 (status=WRONG, page=abc 등)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request
    ) {
        return toResponse(ErrorCode.INVALID_INPUT, "'" + e.getName() + "' 값이 올바르지 않아요.", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException e, HttpServletRequest request
    ) {
        return toResponse(ErrorCode.INVALID_INPUT, "'" + e.getParameterName() + "' 값이 필요해요.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException e, HttpServletRequest request
    ) {
        return toResponse(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request
    ) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage(), request);
    }

    // 그 외 예상치 못한 모든 예외는 500으로 처리해요
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request
    ) {
        log.error("[UnhandledException] {} {} | {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        return toResponse(ErrorCode.COMMON_INTERNAL_SERVER_ERROR,
                ErrorCode.COMMON_INTERNAL_SERVER_ERROR.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, String message, HttpServletRequest request) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, message, request));
    }
}
