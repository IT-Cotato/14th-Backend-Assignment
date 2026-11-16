package com.cotato.backend14th.domain.study.controller;

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

import com.cotato.backend14th.domain.study.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.study.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.study.dto.StudyPostIdResponse;
import com.cotato.backend14th.domain.study.dto.StudyPostSummaryResponse;
import com.cotato.backend14th.domain.study.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.study.service.StudyPostService;
import com.cotato.backend14th.domain.user.entity.Part;
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

@Tag(name = "StudyPost API", description = "스터디 모집글 API")
@RestController
@RequestMapping("/api/study-posts")
@RequiredArgsConstructor
public class StudyPostController {

    private final StudyPostService studyPostService;

    @Operation(summary = "모집글 작성", description = "토큰의 회원을 작성자로 모집글을 작성해요. 처음 상태는 RECRUITING이에요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패 (COMMON-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰 없음 / 유효하지 않음 (COMMON-004)")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudyPostCreateRequest request
    ) {
        return ResponseUtils.created(studyPostService.create(userId, request));
    }

    @Operation(summary = "모집글 목록", description = "키워드(제목, 내용), 모집 상태, 작성자 파트로 거를 수 있어요. 조건은 함께 쓸 수 있고 최신순이에요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "status/part 값이 올바르지 않음, page/size 범위 초과 (COMMON-003)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudyPostSummaryResponse>>> getList(
            @Parameter(description = "제목 또는 내용에 포함된 키워드", example = "알고리즘")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "모집 상태 (RECRUITING, CLOSED)", example = "RECRUITING")
            @RequestParam(required = false) StudyStatus status,
            @Parameter(description = "작성자의 파트 (PM, DE, BE, FE)", example = "BE")
            @RequestParam(required = false) Part part,
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (1~100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return ResponseUtils.ok(studyPostService.getList(keyword, status, part, page, size));
    }

    @Operation(summary = "모집글 상세", description = "토큰이 있으면 작성자 여부(isAuthor)를 판단해요. 비로그인이면 isAuthor는 false예요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글이 없음 (STUDY-001)")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostDetailResponse>> getDetail(
            @Parameter(description = "모집글 ID", example = "10") @PathVariable Long postId,
            @AuthenticationPrincipal Long userId // 비로그인이면 null
    ) {
        return ResponseUtils.ok(studyPostService.getDetail(postId, userId));
    }

    @Operation(summary = "모집글 수정", description = "보낸 필드만 바꿔요. 모집 상태는 RECRUITING ↔ CLOSED 자유롭게 바꿀 수 있어요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패 (COMMON-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰 없음 / 유효하지 않음 (COMMON-004)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님 (STUDY-002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글이 없음 (STUDY-001)")
    })
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> update(
            @Parameter(description = "모집글 ID", example = "10") @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudyPostUpdateRequest request
    ) {
        return ResponseUtils.ok(studyPostService.update(postId, userId, request));
    }

    @Operation(summary = "모집글 삭제", description = "글과 달린 댓글을 함께 삭제해요. 본문 없이 204를 내려줘요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰 없음 / 유효하지 않음 (COMMON-004)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님 (STUDY-002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글이 없음 (STUDY-001)")
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "모집글 ID", example = "10") @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ) {
        studyPostService.delete(postId, userId);
        return ResponseUtils.noContent();
    }
}
