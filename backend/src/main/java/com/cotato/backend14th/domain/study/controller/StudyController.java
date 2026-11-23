package com.cotato.backend14th.domain.study.controller;

import com.cotato.backend14th.domain.study.dto.StudyDtos.*;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.study.service.StudyService;
import com.cotato.backend14th.domain.user.entity.Part;
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
@RequestMapping("/api/study-posts")
@RequiredArgsConstructor
public class StudyController {
    private final StudyService studies;

    @PostMapping
    public ResponseEntity<ApiResponse<Id>> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Create request) {
        return ResponseUtils.created(studies.create(CurrentUser.id(jwt), request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Summary>>> list(
            @RequestParam(defaultValue = "") String keyword, @RequestParam(required = false) StudyStatus status,
            @RequestParam(required = false) Part part, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseUtils.ok(studies.list(keyword, status, part, page, size));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<Detail>> detail(@PathVariable long postId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseUtils.ok(studies.detail(postId, CurrentUser.id(jwt)));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<Id>> update(@PathVariable long postId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Update request) {
        return ResponseUtils.ok(studies.update(postId, CurrentUser.id(jwt), request));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable long postId, @AuthenticationPrincipal Jwt jwt) {
        studies.delete(postId, CurrentUser.id(jwt));
        return ResponseEntity.noContent().build();
    }
}
