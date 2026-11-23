package com.cotato.backend14th.domain.studypost.controller;

import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostPageResponse;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.service.StudyPostService;
import com.cotato.backend14th.domain.user.entity.Part;
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
public class StudyPostController {

    private final StudyPostService studyPostService;
    private final OptionalJwtAuthentication optionalJwtAuthentication;

    @Operation(summary = "스터디 모집글 작성")
    @PostMapping("/api/study-posts")
    public ResponseEntity<ApiResponse<StudyPostCreateResponse>> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StudyPostCreateRequest request) {
        return ResponseUtils.created(studyPostService.create(jwt, request));
    }

    @Operation(summary = "스터디 모집글 목록 조회")
    @GetMapping("/api/study-posts")
    public ResponseEntity<ApiResponse<StudyPostPageResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String part,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        StudyStatus studyStatus;
        Part authorPart;
        PageRequest pageable;
        try {
            studyStatus = status == null ? null : StudyStatus.valueOf(status);
            authorPart = part == null ? null : Part.valueOf(part);
            pageable = PageRequest.of(page == null ? 0 : Integer.parseInt(page),
                    size == null ? 10 : Integer.parseInt(size));
        } catch (IllegalArgumentException e) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }

        return ResponseUtils.ok(studyPostService.list(keyword, studyStatus, authorPart, pageable));
    }

    @Operation(summary = "스터디 모집글 상세 조회")
    @GetMapping("/api/study-posts/{postId}")
    public ResponseEntity<ApiResponse<StudyPostDetailResponse>> detail(
            @PathVariable String postId, HttpServletRequest request) {
        return ResponseUtils.ok(studyPostService.detail(parsePostId(postId), optionalJwtAuthentication.resolve(request)));
    }

    @Operation(summary = "스터디 모집글 수정")
    @PatchMapping("/api/study-posts/{postId}")
    public ResponseEntity<ApiResponse<StudyPostUpdateResponse>> update(
            @PathVariable String postId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StudyPostUpdateRequest request) {
        return ResponseUtils.ok(studyPostService.update(parsePostId(postId), jwt, request));
    }

    @Operation(summary = "스터디 모집글 삭제")
    @DeleteMapping("/api/study-posts/{postId}")
    public ResponseEntity<Void> delete(@PathVariable String postId, @AuthenticationPrincipal Jwt jwt) {
        studyPostService.delete(parsePostId(postId), jwt);
        return ResponseEntity.noContent().build();
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
