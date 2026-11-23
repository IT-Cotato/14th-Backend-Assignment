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
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "StudyPost API", description = "스터디 모집글 API")
@RestController
@RequestMapping("/api/study-posts")
@RequiredArgsConstructor
public class StudyPostController {

    private final StudyPostService studyPostService;

    @Operation(summary = "모집글 작성", description = "스터디 모집글을 작성해요. 처음 상태는 RECRUITING이에요.")
    @PostMapping
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudyPostCreateRequest request) {
        return ResponseUtils.created(studyPostService.create(userId, request));
    }

    @Operation(summary = "모집글 목록", description = "키워드(제목·내용), 모집 상태, 작성자 파트로 거르고 최신순으로 페이징해요.")
    @SecurityRequirements // 공개 API라 Swagger 자물쇠 표시를 빼요
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudyPostSummaryResponse>>> getList(
            @Parameter(description = "제목·내용에 포함된 키워드", example = "알고리즘")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "모집 상태 (RECRUITING, CLOSED). 없으면 전체", example = "RECRUITING")
            @RequestParam(required = false) StudyStatus status,
            @Parameter(description = "작성자 파트 (PM, DE, BE, FE). 없으면 전체", example = "BE")
            @RequestParam(required = false) Part part,
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (1~100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseUtils.ok(studyPostService.getList(keyword, status, part, page, size));
    }

    @Operation(summary = "모집글 상세", description = "모집글 하나를 조회해요. 토큰 없이도 호출되고, 로그인하지 않았으면 isAuthor는 false예요.")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostDetailResponse>> getDetail(
            @Parameter(description = "조회할 모집글 ID", example = "1")
            @PathVariable Long postId,
            // 비로그인이면 principal이 "anonymousUser" 문자열이라 Long 자리에 null이 들어와요
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return ResponseUtils.ok(studyPostService.getDetail(postId, userId));
    }

    @Operation(summary = "모집글 수정", description = "작성자만 수정할 수 있어요. 바꿀 필드만 보내요. (모집 상태 변경 포함)")
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> update(
            @Parameter(description = "수정할 모집글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudyPostUpdateRequest request) {
        return ResponseUtils.ok(studyPostService.update(postId, userId, request));
    }

    @Operation(summary = "모집글 삭제", description = "작성자만 삭제할 수 있어요. 달린 댓글도 함께 삭제돼요.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "삭제할 모집글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        studyPostService.delete(postId, userId);
        return ResponseEntity.noContent().build(); // 본문 없는 204는 ApiResponse로 감싸지 않아요
    }
}
