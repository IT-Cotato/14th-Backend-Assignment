
package com.cotato.backend14th.global.exception;

import com.cotato.backend14th.global.common.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 직접 발생시킨 비즈니스 예외 처리
    @ExceptionHandler(Backend14thException.class)
    public ResponseEntity<ErrorResponse> handleBackend14thException(
            Backend14thException e,
            HttpServletRequest request
    ) {
        log.warn("[Backend14thException] {} {} | code={} | message={}",
                request.getMethod(),
                request.getRequestURI(),
                e.getErrorCode().getCode(),
                e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e.getErrorCode(), request));
    }

    // 2. @Valid 입력값 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        log.warn("[ValidationException] {} {} | errorCount={}",
                request.getMethod(),
                request.getRequestURI(),
                e.getBindingResult().getErrorCount());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, request));
    }

    // 3. JSON 형식 오류 및 Enum 변환 실패 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        log.warn("[HttpMessageNotReadableException] {} {}",
                request.getMethod(),
                request.getRequestURI());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, request));
    }

    // 4. 쿼리 파라미터·경로 변수 타입 오류 처리 (예: status=FOO, postId=abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        log.warn("[MethodArgumentTypeMismatchException] {} {} | parameter={}",
                request.getMethod(),
                request.getRequestURI(),
                e.getName());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, request));
    }

    // 5. 쿼리 파라미터 검증 실패 처리 (예: page=-1, size=0)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException e,
            HttpServletRequest request
    ) {
        log.warn("[HandlerMethodValidationException] {} {}",
                request.getMethod(),
                request.getRequestURI());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, request));
    }

    // 6. 존재하지 않는 URL 요청 처리 (매핑된 API도, 정적 리소스도 없는 경우)
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            Exception e,
            HttpServletRequest request
    ) {
        log.warn("[NotFound] {} {}",
                request.getMethod(),
                request.getRequestURI());

        return ResponseEntity
                .status(ErrorCode.COMMON_NOT_FOUND.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_NOT_FOUND, request));
    }

    // 7. URL은 있지만 지원하지 않는 HTTP 메서드 요청 처리 (예: PUT /api/study-posts/1)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        log.warn("[MethodNotAllowed] {} {} | supported={}",
                request.getMethod(),
                request.getRequestURI(),
                e.getSupportedHttpMethods());

        // 405 응답에는 허용 메서드를 Allow 헤더로 알려줘요.
        HttpHeaders headers = new HttpHeaders();
        Set<HttpMethod> supportedMethods = e.getSupportedHttpMethods();
        if (supportedMethods != null && !supportedMethods.isEmpty()) {
            headers.setAllow(supportedMethods);
        }

        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .headers(headers)
                .body(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, request));
    }

    // 8. 예상하지 못한 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("[UnhandledException] {} {} | {}",
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage(),
                e);

        return ResponseEntity
                .status(ErrorCode.COMMON_INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.COMMON_INTERNAL_SERVER_ERROR,
                        request
                ));
    }
}
