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
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON-003", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON-004", "인증이 필요하거나 유효하지 않은 토큰입니다."),

    // USER
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER-001", "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USER-002", "이메일 또는 비밀번호가 올바르지 않습니다."),

    // STUDY
    STUDY_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY-001", "존재하지 않는 스터디 모집글입니다."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "STUDY-002", "모집글 작성자만 수정하거나 삭제할 수 있습니다."),
    STUDY_POST_CLOSED(HttpStatus.CONFLICT, "STUDY-003", "모집이 마감된 글에는 댓글을 작성할 수 없습니다."),

    // COMMENT
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT-001", "존재하지 않는 댓글입니다."),
    NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "COMMENT-002", "댓글 작성자만 수정하거나 삭제할 수 있습니다."),

    // BOOK
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK-001", "책을 찾을 수 없습니다.");

    // 나중에 도메인이 늘어나면 이렇게 그룹을 추가하면 돼요
    // APPLICANT_NOT_FOUND(HttpStatus.NOT_FOUND, "APPLICANT-001", "지원자를 찾을 수 없습니다."),
    // DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT-001", "서류를 찾을 수 없습니다."),

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}