package com.cotato.backend14th.domain.study;

import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment API")
@RestController
public class StudyPostCommentController {

    private final StudyPostCommentService commentService;

    public StudyPostCommentController(StudyPostCommentService commentService) {
        this.commentService = commentService;
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) return null;
        return (Long) authentication.getPrincipal();
    }

    private Long requireUserId(Authentication authentication) {
        Long userId = currentUserId(authentication);
        if (userId == null) throw new Backend14thException(ErrorCode.UNAUTHORIZED);
        return userId;
    }

    @Operation(summary = "댓글 작성")
    @PostMapping("/api/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CreateCommentResponse>> create(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseUtils.created(commentService.create(requireUserId(authentication), postId, request));
    }

    @Operation(summary = "댓글 목록")
    @GetMapping("/api/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentListResponse>> list(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseUtils.ok(commentService.list(postId, currentUserId(authentication), page, size));
    }

    @Operation(summary = "댓글 수정")
    @PatchMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<CreateCommentResponse>> update(
            Authentication authentication,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseUtils.ok(commentService.update(requireUserId(authentication), commentId, request));
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long commentId) {
        commentService.delete(requireUserId(authentication), commentId);
        return ResponseEntity.noContent().build();
    }
}
