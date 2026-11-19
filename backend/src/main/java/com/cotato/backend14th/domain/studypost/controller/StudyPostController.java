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
import com.cotato.backend14th.global.common.response.ErrorResponse;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "StudyPost API", description = "스터디 모집글 API")
@RestController
@RequestMapping("/api/study-posts")
@RequiredArgsConstructor
public class StudyPostController {

    private final StudyPostService studyPostService;

    @Operation(summary = "모집글 작성", description = "새 모집글을 작성해요. 처음 상태는 RECRUITING이에요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT: 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 로그인하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudyPostCreateRequest request) {
        return ResponseUtils.created(studyPostService.create(userId, request));
    }

    @Operation(summary = "모집글 목록", description = "키워드(제목·내용), 모집 상태, 작성자 파트로 검색해요. 최신순이에요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT: status, part, page, size 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudyPostSummaryResponse>>> getList(
            @Parameter(description = "제목 또는 내용에 포함된 키워드", example = "알고리즘")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "모집 상태 (RECRUITING, CLOSED)", example = "RECRUITING")
            @RequestParam(value = "status", required = false) StudyStatus status,
            @Parameter(description = "작성자 파트 (PM, DE, BE, FE)", example = "BE")
            @RequestParam(value = "part", required = false) Part part,
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~100)", example = "10")
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseUtils.ok(studyPostService.getList(keyword, status, part, page, size));
    }

    @Operation(summary = "모집글 상세", description = "모집글을 조회해요. 토큰을 보내면 작성자 여부(isAuthor)를 계산해요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "STUDY_POST_NOT_FOUND: 글이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostDetailResponse>> getDetail(
            @Parameter(description = "모집글 ID", example = "1")
            @PathVariable("postId") Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return ResponseUtils.ok(studyPostService.getDetail(postId, userId));
    }

    @Operation(summary = "모집글 수정", description = "바꿀 필드만 보내요. 모집 상태 변경도 여기서 해요. 작성자만 할 수 있어요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_INPUT: 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 로그인하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "NOT_POST_AUTHOR: 작성자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "STUDY_POST_NOT_FOUND: 글이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> update(
            @Parameter(description = "모집글 ID", example = "1")
            @PathVariable("postId") Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudyPostUpdateRequest request) {
        return ResponseUtils.ok(studyPostService.update(postId, userId, request));
    }

    // 명세상 204는 ApiResponse로 감싸지 않고 본문 없이 내려줘요 (controller 규칙의 예외)
    @Operation(summary = "모집글 삭제", description = "모집글을 삭제해요. 작성자만 할 수 있어요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 로그인하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "NOT_POST_AUTHOR: 작성자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "STUDY_POST_NOT_FOUND: 글이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "모집글 ID", example = "1")
            @PathVariable("postId") Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        studyPostService.delete(postId, userId);
        return ResponseEntity.noContent().build();
    }
}
