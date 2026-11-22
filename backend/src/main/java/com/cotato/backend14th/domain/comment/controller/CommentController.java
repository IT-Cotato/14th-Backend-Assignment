package com.cotato.backend14th.domain.comment.controller;

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

import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.service.StudyPostCommentService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

// 작성·목록은 /api/study-posts/{postId}/comments, 수정·삭제는 /api/comments/{commentId}라 공통 접두사만 둬요
@Tag(name = "Comment API", description = "모집글 댓글(= 스터디 신청) API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final StudyPostCommentService commentService;

    @Operation(summary = "댓글(신청) 작성", description = "모집 중인 글에 댓글을 달아요. 마감된 글이면 409예요.")
    @PostMapping("/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentIdResponse>> create(
            @Parameter(description = "댓글을 달 모집글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseUtils.created(commentService.create(postId, userId, request));
    }

    @Operation(summary = "댓글 목록", description = "오래된 순으로 페이징해요. 토큰 없이도 호출되고, 로그인하지 않았으면 isAuthor는 false예요.")
    @GetMapping("/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getList(
            @Parameter(description = "모집글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (1~100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            // 비로그인이면 principal이 "anonymousUser" 문자열이라 Long 자리에 null이 들어와요
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return ResponseUtils.ok(commentService.getList(postId, userId, page, size));
    }

    @Operation(summary = "댓글 수정", description = "작성자만 수정할 수 있어요.")
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentIdResponse>> update(
            @Parameter(description = "수정할 댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseUtils.ok(commentService.update(commentId, userId, request));
    }

    @Operation(summary = "댓글 삭제", description = "작성자만 삭제할 수 있어요.")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "삭제할 댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        commentService.delete(commentId, userId);
        return ResponseEntity.noContent().build(); // 본문 없는 204는 ApiResponse로 감싸지 않아요
    }
}
