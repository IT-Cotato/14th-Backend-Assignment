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

@Tag(name = "StudyPost API")
@RestController
@RequestMapping("/api/study-posts")
public class StudyPostController {

    private final StudyPostService studyPostService;

    public StudyPostController(StudyPostService studyPostService) {
        this.studyPostService = studyPostService;
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return (Long) authentication.getPrincipal();
    }

    private Long requireUserId(Authentication authentication) {
        Long userId = currentUserId(authentication);
        if (userId == null) {
            throw new Backend14thException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    @Operation(summary = "모집글 작성")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateStudyPostResponse>> create(
            Authentication authentication,
            @Valid @RequestBody CreateStudyPostRequest request) {
        return ResponseUtils.created(studyPostService.create(requireUserId(authentication), request));
    }

    @Operation(summary = "모집글 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<StudyPostListResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String part,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseUtils.ok(studyPostService.list(keyword, status, part, page, size));
    }

    @Operation(summary = "모집글 상세")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostDetailResponse>> get(
            @PathVariable Long postId,
            Authentication authentication) {
        return ResponseUtils.ok(studyPostService.get(postId, currentUserId(authentication)));
    }

    @Operation(summary = "모집글 수정")
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<UpdateStudyPostResponse>> update(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody UpdateStudyPostRequest request) {
        return ResponseUtils.ok(studyPostService.update(requireUserId(authentication), postId, request));
    }

    @Operation(summary = "모집글 삭제")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long postId) {
        studyPostService.delete(requireUserId(authentication), postId);
        return ResponseEntity.noContent().build();
    }
}
