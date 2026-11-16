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
import com.cotato.backend14th.domain.comment.service.CommentService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

// URL이 /api/study-posts/{postId}/comments, /api/comments/{commentId} 두 갈래라 클래스에는 /api까지만 붙여요
@Tag(name = "Comment API", description = "댓글(스터디 신청) API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "모집글에 댓글(신청)을 작성해요. 모집이 마감된(CLOSED) 글에는 쓸 수 없어요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패 (COMMON-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰 없음 / 유효하지 않음 (COMMON-004)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글이 없음 (STUDY-001)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "마감된 글 (STUDY-003)")
    })
    @PostMapping("/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentIdResponse>> create(
            @Parameter(description = "모집글 ID", example = "10") @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommentRequest request
    ) {
        return ResponseUtils.created(commentService.create(postId, userId, request));
    }

    @Operation(summary = "댓글 목록", description = "오래된 순이에요. 토큰이 있으면 작성자 여부(isAuthor)를 판단하고, 비로그인이면 false예요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "page/size 범위 초과 (COMMON-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글이 없음 (STUDY-001)")
    })
    @GetMapping("/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getList(
            @Parameter(description = "모집글 ID", example = "10") @PathVariable Long postId,
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (1~100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal Long userId // 비로그인이면 null
    ) {
        return ResponseUtils.ok(commentService.getList(postId, userId, page, size));
    }

    @Operation(summary = "댓글 수정", description = "작성자만 수정할 수 있어요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패 (COMMON-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰 없음 / 유효하지 않음 (COMMON-004)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님 (COMMENT-002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글이 없음 (COMMENT-001)")
    })
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentIdResponse>> update(
            @Parameter(description = "댓글 ID", example = "5") @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommentRequest request
    ) {
        return ResponseUtils.ok(commentService.update(commentId, userId, request));
    }

    @Operation(summary = "댓글 삭제", description = "작성자만 삭제할 수 있어요. 본문 없이 204를 내려줘요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰 없음 / 유효하지 않음 (COMMON-004)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님 (COMMENT-002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글이 없음 (COMMENT-001)")
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "댓글 ID", example = "5") @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId
    ) {
        commentService.delete(commentId, userId);
        return ResponseUtils.noContent();
    }
}
