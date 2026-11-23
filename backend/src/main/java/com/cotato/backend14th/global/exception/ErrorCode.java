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

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON-003", "입력값을 확인해 주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON-004", "로그인이 필요해요."),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER-001", "이미 가입된 이메일이에요."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USER-002", "이메일 또는 비밀번호가 틀렸어요."),

    STUDY_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-001", "존재하지 않는 스터디 모집글이에요."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "STUDY-002", "모집글 작성자만 변경할 수 있어요."),
    STUDY_POST_CLOSED(HttpStatus.CONFLICT, "STUDY-003", "모집이 마감된 글이에요."),

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT-001", "존재하지 않는 댓글이에요."),
    NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "COMMENT-002", "댓글 작성자만 변경할 수 있어요."),

    // BOOK
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK-001", "책을 찾을 수 없습니다.");

    // 나중에 도메인이 늘어나면 이렇게 그룹을 추가하면 돼요
    // APPLICANT_NOT_FOUND(HttpStatus.NOT_FOUND, "APPLICANT-001", "지원자를 찾을 수 없습니다."),
    // DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT-001", "서류를 찾을 수 없습니다."),

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}