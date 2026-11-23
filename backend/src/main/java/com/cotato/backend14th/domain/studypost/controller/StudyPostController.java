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
import com.cotato.backend14th.domain.studypost.dto.StudyPostSummaryResponse;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "Study Post API", description = "스터디 모집글 API")
@RestController
@RequestMapping("/api/study-posts")
@RequiredArgsConstructor
public class StudyPostController {

    private final StudyPostService studyPostService;

    @Operation(summary = "모집글 작성", description = "처음 만들면 모집 상태는 RECRUITING이에요.")
    @PostMapping
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudyPostCreateRequest request) {
        return ResponseUtils.created(studyPostService.create(userId, request));
    }

    @Operation(summary = "모집글 목록",
            description = "키워드(제목, 내용), 모집 상태, 작성자 파트로 거르고 최신순으로 페이징해요. 조건은 함께 쓸 수 있어요.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudyPostSummaryResponse>>> search(
            @Parameter(description = "제목, 내용에 포함된 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "RECRUITING 또는 CLOSED") @RequestParam(required = false) StudyStatus status,
            @Parameter(description = "작성자의 파트: PM, DE, BE, FE") @RequestParam(required = false) Part part,
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (1~100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseUtils.ok(studyPostService.search(keyword, status, part, page, size));
    }

    @Operation(summary = "모집글 상세", description = "공개 API예요. 토큰을 보내면 isAuthor로 작성자 본인인지 알려줘요.")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostDetailResponse>> getDetail(
            @Parameter(description = "모집글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return ResponseUtils.ok(studyPostService.getDetail(postId, userId));
    }

    @Operation(summary = "모집글 수정", description = "보낸 필드만 바꿔요. 모집 상태 변경도 여기서 해요. 작성자만 할 수 있어요.")
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> update(
            @Parameter(description = "모집글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudyPostUpdateRequest request) {
        return ResponseUtils.ok(studyPostService.update(postId, userId, request));
    }

    @Operation(summary = "모집글 삭제", description = "달린 댓글도 함께 지워요. 작성자만 할 수 있어요. 본문 없이 204를 내려줘요.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "모집글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        studyPostService.delete(postId, userId);
        return ResponseEntity.noContent().build();
    }
}
