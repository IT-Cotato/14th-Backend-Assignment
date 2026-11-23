package com.cotato.backend14th.domain.comment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.cotato.backend14th.domain.comment.dto.request.CommentCreateRequest;
import com.cotato.backend14th.domain.comment.dto.response.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.response.CommentListItemResponse;
import com.cotato.backend14th.domain.comment.service.CommentService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Comment API", description = "댓글(스터디 신청) 관련 API")
@RestController
@RequestMapping("/api/study-posts/{postId}/comments")
@RequiredArgsConstructor
public class StudyPostCommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "댓글이 곧 스터디 신청이에요.")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentIdResponse>> create(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseUtils.created(commentService.create(postId, userId, request));
    }

    @Operation(summary = "댓글 목록", description = "오래된 순으로 보여줘요.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CommentListItemResponse>>> list(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseUtils.ok(commentService.list(postId, userId, page, size));
    }
}
