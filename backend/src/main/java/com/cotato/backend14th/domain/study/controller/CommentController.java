package com.cotato.backend14th.domain.study.controller;

import com.cotato.backend14th.domain.study.dto.CommentDtos.*;
import com.cotato.backend14th.domain.study.service.CommentService;
import com.cotato.backend14th.global.common.response.*;
import com.cotato.backend14th.global.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService comments;

    @PostMapping("/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Id>> create(@PathVariable long postId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Input input) {
        return ResponseUtils.created(comments.create(postId, CurrentUser.id(jwt), input));
    }

    @GetMapping("/study-posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<Item>>> list(@PathVariable long postId, @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseUtils.ok(comments.list(postId, CurrentUser.id(jwt), page, size));
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Id>> update(@PathVariable long commentId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Input input) {
        return ResponseUtils.ok(comments.update(commentId, CurrentUser.id(jwt), input));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable long commentId, @AuthenticationPrincipal Jwt jwt) {
        comments.delete(commentId, CurrentUser.id(jwt));
        return ResponseEntity.noContent().build();
    }
}
