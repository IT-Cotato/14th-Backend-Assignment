package com.cotato.backend14th.domain.comment.controller;

import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.service.CommentService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Comment API", description = "댓글(스터디 신청) API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "로그인한 사용자가 댓글(스터디 신청)을 달아요. 모집이 마감된 글에는 쓸 수 없어요.")
    @PostMapping("/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentIdResponse>> create(
            @Parameter(description = "모집글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommentRequest request
            ) {
        return ResponseUtils.created(commentService.create(postId, userId, request));
    }

    @Operation(summary = "댓글 목록", description = "로그인하지 않아도 볼 수 있어요. 오래된 순이에요.")
    @GetMapping("/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getList(
            @Parameter(description = "모집글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseUtils.ok(commentService.getList(postId, userId, page, size));
    }

    @Operation(summary = "댓글 수정", description = "작성자만 수정할 수 있어요.")
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentIdResponse>> update(
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseUtils.ok(commentService.update(commentId, userId, request));
    }

    @Operation(summary = "댓글 삭제", description = "작성자만 삭제할 수 있어요.")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        commentService.delete(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
