package com.cotato.backend14th.domain.comment.controller;

import com.cotato.backend14th.domain.comment.dto.CommentCreateRequest;
import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.dto.CommentUpdateRequest;
import com.cotato.backend14th.domain.comment.service.CommentService;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import com.cotato.backend14th.global.config.SwaggerConfig;
import com.cotato.backend14th.global.security.JwtUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 작성·목록은 모집글 하위 경로, 수정·삭제는 댓글 경로를 사용해서 클래스 레벨 경로를 두지 않아요.
@Tag(name = "댓글 API", description = "스터디 모집글 댓글(스터디 신청) 작성, 조회, 수정, 삭제 API예요.")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "댓글 작성 (스터디 신청)",
            description = "로그인한 사용자가 모집글에 댓글을 작성해요. 모집이 마감(CLOSED)된 글에는 작성할 수 없어요.",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    )
    @PostMapping("/api/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentIdResponse>> createComment(
            @Parameter(description = "댓글을 작성할 모집글 ID예요.", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseUtils.created(
                commentService.create(postId, JwtUtils.getUserId(jwt), request)
        );
    }

    @Operation(
            summary = "댓글 목록 조회",
            description = "모집글의 댓글을 오래된 순으로 페이징해서 조회해요. 로그인 없이 조회할 수 있고, 로그인하면 내 댓글은 isAuthor가 true예요."
    )
    @GetMapping("/api/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @Parameter(description = "댓글을 조회할 모집글 ID예요.", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "페이지 번호예요. 0부터 시작해요.", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기예요. 1~100까지 가능해요.", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseUtils.ok(
                commentService.getComments(postId, JwtUtils.getUserIdOrNull(jwt), page, size)
        );
    }

    @Operation(
            summary = "댓글 수정",
            description = "작성자 본인만 댓글 내용을 수정할 수 있어요.",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    )
    @PatchMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentIdResponse>> updateComment(
            @Parameter(description = "수정할 댓글 ID예요.", example = "1")
            @PathVariable Long commentId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ResponseUtils.ok(
                commentService.update(commentId, JwtUtils.getUserId(jwt), request)
        );
    }

    @Operation(
            summary = "댓글 삭제",
            description = "작성자 본인만 댓글을 삭제할 수 있어요. 성공하면 본문 없이 204를 반환해요.",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    )
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "삭제할 댓글 ID예요.", example = "1")
            @PathVariable Long commentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        commentService.delete(commentId, JwtUtils.getUserId(jwt));

        return ResponseEntity.noContent().build();
    }
}
