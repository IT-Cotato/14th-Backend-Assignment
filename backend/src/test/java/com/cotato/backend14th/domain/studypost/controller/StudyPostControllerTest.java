package com.cotato.backend14th.domain.studypost.controller;

import com.cotato.backend14th.domain.auth.service.TokenRevocationService;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostIdResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostSummaryResponse;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.service.StudyPostService;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.config.CorsConfig;
import com.cotato.backend14th.global.config.JwtConfig;
import com.cotato.backend14th.global.config.SecurityConfig;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.security.JwtAuthenticationEntryPoint;
import com.cotato.backend14th.global.security.RevokedTokenValidator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 모집글 API의 인증, 입력값 검증, HTTP 상태와 응답 형식을 DB 없이 확인해요.
@WebMvcTest(controllers = StudyPostController.class)
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class,
        JwtAuthenticationEntryPoint.class, RevokedTokenValidator.class})
@TestPropertySource(properties = "JWT_SECRET=test-only-jwt-secret-key-at-least-32-bytes!!")
class StudyPostControllerTest {

    private static final AuthorResponse AUTHOR = new AuthorResponse("길동이", 14, Part.BE);
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 11, 8, 12, 0, 0, 123_456_000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private StudyPostService studyPostService;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // ===== 작성 =====

    @Test
    @DisplayName("로그인하면 작성할 수 있고 201과 postId를 반환해요")
    void create() throws Exception {
        given(studyPostService.create(eq(1L), any())).willReturn(new StudyPostIdResponse(10L));

        mockMvc.perform(post("/api/study-posts")
                        .header("Authorization", bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"알고리즘 스터디\",\"content\":\"주 2회\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.postId").value(10));
    }

    @Test
    @DisplayName("토큰 없이 작성·수정·삭제하면 401이에요")
    void writeApis_noToken() throws Exception {
        expectError(mockMvc.perform(post("/api/study-posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"content\":\"c\"}")), 401, "COMMON-004");
        expectError(mockMvc.perform(patch("/api/study-posts/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\"}")), 401, "COMMON-004");
        expectError(mockMvc.perform(delete("/api/study-posts/10")), 401, "COMMON-004");

        then(studyPostService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제목·내용이 비었거나 너무 길면 400이에요")
    void create_invalidInput() throws Exception {
        String[] invalidBodies = {
                "{\"title\":\"\",\"content\":\"내용\"}",
                "{\"title\":\"   \",\"content\":\"내용\"}",
                "{\"content\":\"내용\"}",
                "{\"title\":\"제목\",\"content\":\"\"}",
                "{\"title\":\"" + "가".repeat(51) + "\",\"content\":\"내용\"}",
                "{\"title\":\"제목\",\"content\":\"" + "가".repeat(2001) + "\"}",
                "{\"title\":"
        };
        for (String body : invalidBodies) {
            expectError(mockMvc.perform(post("/api/study-posts")
                    .header("Authorization", bearer(1L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)), 400, "COMMON-003");
        }
        then(studyPostService).should(never()).create(anyLong(), any());
    }

    @Test
    @DisplayName("제목 50자, 내용 2000자는 경계값으로 허용해요")
    void create_boundary() throws Exception {
        given(studyPostService.create(eq(1L), any())).willReturn(new StudyPostIdResponse(10L));

        mockMvc.perform(post("/api/study-posts")
                        .header("Authorization", bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "가".repeat(50) + "\",\"content\":\"" + "가".repeat(2000) + "\"}"))
                .andExpect(status().isCreated());
    }

    // ===== 목록 =====

    @Test
    @DisplayName("목록은 로그인 없이 조회되고 기본값은 page=0, size=10이에요")
    void list_publicWithDefaults() throws Exception {
        given(studyPostService.getStudyPosts(null, null, null, 0, 10)).willReturn(new PageResponse<>(
                List.of(new StudyPostSummaryResponse(10L, "알고리즘 스터디", StudyStatus.RECRUITING, AUTHOR, 0, TIME)),
                0, 10, 1, 1
        ));

        mockMvc.perform(get("/api/study-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].postId").value(10))
                .andExpect(jsonPath("$.data.content[0].studyStatus").value("RECRUITING"))
                .andExpect(jsonPath("$.data.content[0].author.nickname").value("길동이"))
                .andExpect(jsonPath("$.data.content[0].author.generation").value(14))
                .andExpect(jsonPath("$.data.content[0].author.part").value("BE"))
                .andExpect(jsonPath("$.data.content[0].commentCount").value(0))
                .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-11-08T12:00:00"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("검색 조건을 서비스로 그대로 전달해요")
    void list_passesFilters() throws Exception {
        given(studyPostService.getStudyPosts(any(), any(), any(), anyInt(), anyInt()))
                .willReturn(new PageResponse<>(List.of(), 2, 5, 0, 0));

        mockMvc.perform(get("/api/study-posts")
                        .param("keyword", "알고리즘")
                        .param("status", "CLOSED")
                        .param("part", "FE")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        then(studyPostService).should().getStudyPosts("알고리즘", StudyStatus.CLOSED, Part.FE, 2, 5);
    }

    @Test
    @DisplayName("잘못된 상태·파트·페이지 값은 400이에요")
    void list_invalidParams() throws Exception {
        String[][] invalidParams = {
                {"status", "OPEN"},
                {"part", "XX"},
                {"page", "-1"},
                {"page", "abc"},
                {"size", "0"},
                {"size", "101"}
        };
        for (String[] param : invalidParams) {
            expectError(mockMvc.perform(get("/api/study-posts").param(param[0], param[1])), 400, "COMMON-003");
        }
        then(studyPostService).shouldHaveNoInteractions();
    }

    // ===== 상세 =====

    @Test
    @DisplayName("비로그인 상세 조회는 viewerId 없이 서비스에 전달되고 isAuthor=false예요")
    void detail_guest() throws Exception {
        given(studyPostService.getStudyPost(10L, null)).willReturn(detail(false));

        mockMvc.perform(get("/api/study-posts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(10))
                .andExpect(jsonPath("$.data.content").value("주 2회 백준"))
                .andExpect(jsonPath("$.data.isAuthor").value(false))
                .andExpect(jsonPath("$.data.author.part").value("BE"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-11-08T12:00:00"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-11-08T12:00:00"));

        then(studyPostService).should().getStudyPost(eq(10L), isNull());
    }

    @Test
    @DisplayName("로그인한 작성자가 상세 조회하면 토큰의 사용자 ID로 isAuthor=true를 받아요")
    void detail_author() throws Exception {
        given(studyPostService.getStudyPost(10L, 1L)).willReturn(detail(true));

        mockMvc.perform(get("/api/study-posts/10").header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAuthor").value(true));
    }

    @Test
    @DisplayName("없는 글은 404, 숫자가 아닌 ID는 400이에요")
    void detail_notFoundAndInvalidId() throws Exception {
        given(studyPostService.getStudyPost(999L, null))
                .willThrow(new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        expectError(mockMvc.perform(get("/api/study-posts/999")), 404, "STUDY-001");
        expectError(mockMvc.perform(get("/api/study-posts/abc")), 400, "COMMON-003");
    }

    // ===== 수정 =====

    @Test
    @DisplayName("작성자가 수정하면 200과 postId를 반환해요")
    void update() throws Exception {
        given(studyPostService.update(eq(10L), eq(1L), any())).willReturn(new StudyPostIdResponse(10L));

        mockMvc.perform(patch("/api/study-posts/10")
                        .header("Authorization", bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studyStatus\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(10));
    }

    @Test
    @DisplayName("작성자가 아니면 수정은 403, 없는 글은 404예요")
    void update_forbiddenAndNotFound() throws Exception {
        given(studyPostService.update(eq(10L), eq(2L), any()))
                .willThrow(new Backend14thException(ErrorCode.NOT_POST_AUTHOR));
        given(studyPostService.update(eq(999L), eq(1L), any()))
                .willThrow(new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        expectError(mockMvc.perform(patch("/api/study-posts/10")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"남의 글\"}")), 403, "STUDY-002");
        expectError(mockMvc.perform(patch("/api/study-posts/999")
                .header("Authorization", bearer(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"없는 글\"}")), 404, "STUDY-001");
    }

    @Test
    @DisplayName("수정 시 빈 제목, 50자 초과, 잘못된 상태 값은 400이에요")
    void update_invalidInput() throws Exception {
        String[] invalidBodies = {
                "{\"title\":\"\"}",
                "{\"title\":\"   \"}",
                "{\"content\":\" \"}",
                "{\"title\":\"" + "가".repeat(51) + "\"}",
                "{\"studyStatus\":\"DONE\"}"
        };
        for (String body : invalidBodies) {
            expectError(mockMvc.perform(patch("/api/study-posts/10")
                    .header("Authorization", bearer(1L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)), 400, "COMMON-003");
        }
        then(studyPostService).should(never()).update(anyLong(), anyLong(), any());
    }

    // ===== 삭제 =====

    @Test
    @DisplayName("작성자가 삭제하면 본문 없이 204예요")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/study-posts/10").header("Authorization", bearer(1L)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        then(studyPostService).should().delete(10L, 1L);
    }

    @Test
    @DisplayName("작성자가 아니면 삭제는 403이에요")
    void delete_forbidden() throws Exception {
        willThrow(new Backend14thException(ErrorCode.NOT_POST_AUTHOR))
                .given(studyPostService).delete(10L, 2L);

        expectError(mockMvc.perform(delete("/api/study-posts/10").header("Authorization", bearer(2L))),
                403, "STUDY-002");
    }

    // ===== 도우미 =====

    private StudyPostDetailResponse detail(boolean isAuthor) {
        return new StudyPostDetailResponse(
                10L, "알고리즘 스터디", "주 2회 백준", StudyStatus.RECRUITING, AUTHOR, isAuthor, TIME, TIME
        );
    }

    private void expectError(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.method").exists())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private String bearer(Long userId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
