package com.cotato.backend14th.global.exception;

import com.cotato.backend14th.global.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // Backend14thException을 처리
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

  // 그 외 예상치 못한 모든 예외
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
