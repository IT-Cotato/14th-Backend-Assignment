package com.cotato.backend14th.domain.studypost.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostIdResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostListResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.service.StudyPostService;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "StudyPost API", description = "스터디 모집글 API")
@RestController
@RequestMapping("/api/study-posts")
@RequiredArgsConstructor
public class StudyPostController {

    private final StudyPostService studyPostService;

    @Operation(summary = "모집글 작성", description = "로그인한 사용자가 모집글을 작성해요. 처음에는 모집 중(RECRUITING)이에요.")
    @PostMapping
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudyPostCreateRequest request) {
        return ResponseUtils.created(studyPostService.create(userId, request));
    }

    @Operation(summary = "모집글 목록", description = "키워드, 모집 상태, 작성자 파트로 거르고 최신순으로 보여줘요.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudyPostListResponse>>> getList(
            @Parameter(description = "제목, 내용에 포함된 키워드", example = "알고리즘")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "모집 상태 (RECRUITING, CLOSED)")
            @RequestParam(required = false) StudyStatus status,
            @Parameter(description = "작성자의 파트 (PM, DE, BE, FE)")
            @RequestParam(required = false) Part part,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseUtils.ok(studyPostService.getList(keyword, status, part, page, size));
    }

    @Operation(summary = "모집글 상세", description = "로그인하지 않아도 볼 수 있어요. 로그인했다면 내 글인지(isAuthor)도 알려줘요.")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostDetailResponse>> getDetail(
            @Parameter(description = "모집글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {
        return ResponseUtils.ok(studyPostService.getDetail(postId, userId));
    }

    @Operation(summary = "모집글 수정", description = "작성자만 수정할 수 있어요. 바꿀 필드만 보내면 되고, 모집 상태도 바꿀 수 있어요.")
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> update(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudyPostUpdateRequest request) {
        return ResponseUtils.ok(studyPostService.update(postId, userId, request));
    }

    @Operation(summary = "모집글 삭제", description = "작성자만 삭제할 수 있어요. 달린 댓글도 함께 삭제돼요.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {
        studyPostService.delete(postId, userId);
        return ResponseEntity.noContent().build();
    }
}
