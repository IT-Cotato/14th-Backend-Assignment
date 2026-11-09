package com.cotato.backend14th.domain.studypost.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "스터디 모집글 API", description = "스터디 모집글 작성, 조회, 검색, 수정, 삭제 API예요.")
@RestController
@RequestMapping("/api/study-posts")
@RequiredArgsConstructor
public class StudyPostController {

    private final StudyPostService studyPostService;

    @Operation(
            summary = "모집글 작성",
            description = "로그인한 사용자가 모집글을 작성해요. 처음 상태는 RECRUITING이에요.",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    )
    @PostMapping
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> createStudyPost(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StudyPostCreateRequest request
    ) {
        StudyPostIdResponse response = studyPostService.create(JwtUtils.getUserId(jwt), request);

        return ResponseUtils.created(response);
    }

    @Operation(
            summary = "모집글 목록 조회",
            description = "키워드(제목·내용), 모집 상태, 작성자 파트로 검색하고 최신순으로 페이징해서 조회해요. 로그인 없이 조회할 수 있어요."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudyPostSummaryResponse>>> getStudyPosts(
            @Parameter(description = "제목·내용 검색어예요.", example = "알고리즘")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "모집 상태예요. (RECRUITING, CLOSED)")
            @RequestParam(required = false) StudyStatus status,
            @Parameter(description = "작성자 파트예요. (PM, DE, BE, FE)")
            @RequestParam(required = false) Part part,
            @Parameter(description = "페이지 번호예요. 0부터 시작해요.", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기예요. 1~100까지 가능해요.", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return ResponseUtils.ok(
                studyPostService.getStudyPosts(keyword, status, part, page, size)
        );
    }

    @Operation(
            summary = "모집글 상세 조회",
            description = "모집글 상세 정보를 조회해요. 로그인한 작성자 본인이면 isAuthor가 true예요."
    )
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostDetailResponse>> getStudyPost(
            @Parameter(description = "조회할 모집글 ID예요.", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseUtils.ok(
                studyPostService.getStudyPost(postId, JwtUtils.getUserIdOrNull(jwt))
        );
    }

    @Operation(
            summary = "모집글 수정",
            description = "작성자 본인만 수정할 수 있어요. 전달한 필드(title, content, studyStatus)만 바뀌어요.",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    )
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<StudyPostIdResponse>> updateStudyPost(
            @Parameter(description = "수정할 모집글 ID예요.", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StudyPostUpdateRequest request
    ) {
        return ResponseUtils.ok(
                studyPostService.update(postId, JwtUtils.getUserId(jwt), request)
        );
    }

    @Operation(
            summary = "모집글 삭제",
            description = "작성자 본인만 삭제할 수 있어요. 성공하면 본문 없이 204를 반환해요.",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    )
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deleteStudyPost(
            @Parameter(description = "삭제할 모집글 ID예요.", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        studyPostService.delete(postId, JwtUtils.getUserId(jwt));

        return ResponseEntity.noContent().build();
    }
}
