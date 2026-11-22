package com.cotato.backend14th.domain.comment.controller;

import com.cotato.backend14th.domain.comment.dto.CommentCreateRequest;
import com.cotato.backend14th.domain.comment.dto.CommentCreateResponse;
import com.cotato.backend14th.domain.comment.dto.CommentPageResponse;
import com.cotato.backend14th.domain.comment.dto.CommentUpdateRequest;
import com.cotato.backend14th.domain.comment.dto.CommentUpdateResponse;
import com.cotato.backend14th.domain.comment.service.StudyPostCommentService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.ErrorResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.OptionalJwtAuthentication;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StudyPostCommentController {

    private final StudyPostCommentService commentService;
    private final OptionalJwtAuthentication optionalJwtAuthentication;

    @Operation(summary = "스터디 모집글 댓글 작성")
    @PostMapping("/api/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentCreateResponse>> create(
            @PathVariable String postId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CommentCreateRequest request) {
        return ResponseUtils.created(commentService.create(parsePostId(postId), jwt, request));
    }

    @Operation(summary = "스터디 모집글 댓글 목록 조회")
    @GetMapping("/api/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentPageResponse>> list(
            @PathVariable String postId,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            HttpServletRequest request) {
        PageRequest pageable;
        try {
            pageable = PageRequest.of(page == null ? 0 : Integer.parseInt(page),
                    size == null ? 20 : Integer.parseInt(size));
        } catch (IllegalArgumentException e) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
        return ResponseUtils.ok(commentService.list(parsePostId(postId), optionalJwtAuthentication.resolve(request), pageable));
    }

    @Operation(summary = "스터디 모집글 댓글 수정")
    @PatchMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentUpdateResponse>> update(
            @PathVariable String commentId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CommentUpdateRequest request) {
        return ResponseUtils.ok(commentService.update(parseCommentId(commentId), jwt, request));
    }

    @Operation(summary = "스터디 모집글 댓글 삭제")
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable String commentId, @AuthenticationPrincipal Jwt jwt) {
        commentService.delete(parseCommentId(commentId), jwt);
        return ResponseEntity.noContent().build();
    }

    private long parseCommentId(String commentId) {
        try {
            return Long.parseLong(commentId);
        } catch (NumberFormatException e) {
            throw new Backend14thException(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    private long parsePostId(String postId) {
        try {
            return Long.parseLong(postId);
        } catch (NumberFormatException e) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInput(HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.of(errorCode, request));
    }
}
