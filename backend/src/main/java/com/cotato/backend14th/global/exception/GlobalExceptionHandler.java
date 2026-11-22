// GlobalExceptionHandler.java
package com.cotato.backend14th.global.exception;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
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

    // @Valid 검증 실패. 어떤 필드가 틀렸는지는 로그로만 남기고 응답은 공통 형식을 유지해요
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request
    ) {
        String fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[ValidationException] {} {} | {}",
                request.getMethod(), request.getRequestURI(), fieldErrors);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, request));
    }

    // part: "XX"처럼 Enum·숫자 변환에 실패하면 @Valid 전에 이 예외가 나요. 500이 아니라 입력 오류로 처리해요
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request
    ) {
        log.warn("[HttpMessageNotReadableException] {} {} | {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, request));
    }

    // ?status=FOO, ?part=XX, /api/study-posts/abc처럼 쿼리·경로 값의 타입 변환에 실패한 경우
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request
    ) {
        log.warn("[TypeMismatch] {} {} | {}={}",
                request.getMethod(), request.getRequestURI(), e.getName(), e.getValue());
        return toResponse(ErrorCode.INVALID_INPUT, request);
    }

    // @RequestParam에 붙인 @Min, @Max 같은 검증에 실패한 경우 (예: size=0)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException e, HttpServletRequest request
    ) {
        log.warn("[HandlerMethodValidation] {} {} | {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT, request);
    }

    // 없는 URL. catch-all 핸들러가 500으로 바꾸지 않도록 따로 잡아요
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException e, HttpServletRequest request
    ) {
        log.warn("[NoResourceFound] {} {}", request.getMethod(), request.getRequestURI());
        return toResponse(ErrorCode.COMMON_NOT_FOUND, request);
    }

    // URL은 있지만 지원하지 않는 메서드 (예: PUT /api/study-posts/1)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request
    ) {
        log.warn("[MethodNotSupported] {} {}", request.getMethod(), request.getRequestURI());
        return toResponse(ErrorCode.COMMON_METHOD_NOT_ALLOWED, request);
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, request));
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