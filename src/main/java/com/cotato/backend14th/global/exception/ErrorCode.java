// ErrorCode.java
package com.cotato.backend14th.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // COMMON
    COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-001", "잘못된 요청입니다."),
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-002", "서버 내부 오류가 발생했습니다."),

    // BOOK
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK-001", "책을 찾을 수 없습니다.");

    // 나중에 도메인이 늘어나면 이렇게 그룹을 추가하면 돼요
    // APPLICANT_NOT_FOUND(HttpStatus.NOT_FOUND, "APPLICANT-001", "지원자를 찾을 수 없습니다."),
    // DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT-001", "서류를 찾을 수 없습니다."),

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}