package com.cotato.backend14th.domain.comment.controller;

import com.cotato.backend14th.domain.auth.service.TokenRevocationService;
import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.service.CommentService;
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

// 댓글 API의 인증, 입력값 검증, HTTP 상태와 응답 형식을 DB 없이 확인해요.
@WebMvcTest(controllers = CommentController.class)
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class,
        JwtAuthenticationEntryPoint.class, RevokedTokenValidator.class})
@TestPropertySource(properties = "JWT_SECRET=test-only-jwt-secret-key-at-least-32-bytes!!")
class CommentControllerTest {

    private static final AuthorResponse AUTHOR = new AuthorResponse("철수", 13, Part.FE);
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 11, 8, 13, 0, 0, 500_000_000);
    private static final String BODY = "{\"content\":\"스터디에 참여하고 싶어요.\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // ===== 작성 =====

    @Test
    @DisplayName("로그인하면 댓글을 작성할 수 있고 201과 commentId를 반환해요")
    void create() throws Exception {
        given(commentService.create(eq(10L), eq(2L), any())).willReturn(new CommentIdResponse(5L));

        mockMvc.perform(post("/api/study-posts/10/comments")
                        .header("Authorization", bearer(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.commentId").value(5));
    }

    @Test
    @DisplayName("토큰 없이 작성·수정·삭제하면 401이에요")
    void writeApis_noToken() throws Exception {
        expectError(mockMvc.perform(post("/api/study-posts/10/comments")
                .contentType(MediaType.APPLICATION_JSON).content(BODY)), 401, "COMMON-004");
        expectError(mockMvc.perform(patch("/api/comments/5")
                .contentType(MediaType.APPLICATION_JSON).content(BODY)), 401, "COMMON-004");
        expectError(mockMvc.perform(delete("/api/comments/5")), 401, "COMMON-004");

        then(commentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("모집 마감 글은 409, 없는 글은 404예요")
    void create_closedAndNotFound() throws Exception {
        given(commentService.create(eq(10L), eq(2L), any()))
                .willThrow(new Backend14thException(ErrorCode.STUDY_POST_CLOSED));
        given(commentService.create(eq(999L), eq(2L), any()))
                .willThrow(new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        expectError(mockMvc.perform(post("/api/study-posts/10/comments")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)), 409, "STUDY-003");
        expectError(mockMvc.perform(post("/api/study-posts/999/comments")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)), 404, "STUDY-001");
    }

    @Test
    @DisplayName("작성·수정 시 내용이 비었거나 500자를 넘으면 400이에요")
    void invalidContent() throws Exception {
        String[] invalidBodies = {
                "{\"content\":\"\"}",
                "{\"content\":\"   \"}",
                "{}",
                "{\"content\":\"" + "가".repeat(501) + "\"}",
                "{\"content\":"
        };
        for (String body : invalidBodies) {
            expectError(mockMvc.perform(post("/api/study-posts/10/comments")
                    .header("Authorization", bearer(2L))
                    .contentType(MediaType.APPLICATION_JSON).content(body)), 400, "COMMON-003");
            expectError(mockMvc.perform(patch("/api/comments/5")
                    .header("Authorization", bearer(2L))
                    .contentType(MediaType.APPLICATION_JSON).content(body)), 400, "COMMON-003");
        }
        then(commentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("댓글 500자는 경계값으로 허용해요")
    void create_boundary() throws Exception {
        given(commentService.create(eq(10L), eq(2L), any())).willReturn(new CommentIdResponse(5L));

        mockMvc.perform(post("/api/study-posts/10/comments")
                        .header("Authorization", bearer(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "가".repeat(500) + "\"}"))
                .andExpect(status().isCreated());
    }

    // ===== 목록 =====

    @Test
    @DisplayName("목록은 로그인 없이 조회되고 기본값은 page=0, size=20이에요")
    void list_publicWithDefaults() throws Exception {
        given(commentService.getComments(10L, null, 0, 20)).willReturn(new PageResponse<>(
                List.of(new CommentResponse(5L, "스터디에 참여하고 싶어요.", AUTHOR, false, TIME)),
                0, 20, 1, 1
        ));

        mockMvc.perform(get("/api/study-posts/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].commentId").value(5))
                .andExpect(jsonPath("$.data.content[0].content").value("스터디에 참여하고 싶어요."))
                .andExpect(jsonPath("$.data.content[0].author.nickname").value("철수"))
                .andExpect(jsonPath("$.data.content[0].author.generation").value(13))
                .andExpect(jsonPath("$.data.content[0].author.part").value("FE"))
                .andExpect(jsonPath("$.data.content[0].isAuthor").value(false))
                .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-11-08T13:00:00"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        then(commentService).should().getComments(eq(10L), isNull(), eq(0), eq(20));
    }

    @Test
    @DisplayName("로그인한 사용자가 조회하면 토큰의 사용자 ID로 isAuthor를 계산해요")
    void list_withLogin() throws Exception {
        given(commentService.getComments(10L, 2L, 1, 5)).willReturn(new PageResponse<>(
                List.of(new CommentResponse(5L, "내 댓글", AUTHOR, true, TIME)), 1, 5, 6, 2
        ));

        mockMvc.perform(get("/api/study-posts/10/comments")
                        .header("Authorization", bearer(2L))
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].isAuthor").value(true))
                .andExpect(jsonPath("$.data.page").value(1));
    }

    @Test
    @DisplayName("없는 글은 404, 잘못된 페이지 값이나 ID는 400이에요")
    void list_errors() throws Exception {
        given(commentService.getComments(999L, null, 0, 20))
                .willThrow(new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        expectError(mockMvc.perform(get("/api/study-posts/999/comments")), 404, "STUDY-001");
        expectError(mockMvc.perform(get("/api/study-posts/10/comments").param("page", "-1")), 400, "COMMON-003");
        expectError(mockMvc.perform(get("/api/study-posts/10/comments").param("size", "0")), 400, "COMMON-003");
        expectError(mockMvc.perform(get("/api/study-posts/10/comments").param("size", "101")), 400, "COMMON-003");
        expectError(mockMvc.perform(get("/api/study-posts/abc/comments")), 400, "COMMON-003");
    }

    // ===== 수정 =====

    @Test
    @DisplayName("작성자가 수정하면 200과 commentId, 남의 댓글은 403, 없는 댓글은 404예요")
    void update() throws Exception {
        given(commentService.update(eq(5L), eq(2L), any())).willReturn(new CommentIdResponse(5L));
        given(commentService.update(eq(5L), eq(1L), any()))
                .willThrow(new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR));
        given(commentService.update(eq(999L), eq(2L), any()))
                .willThrow(new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));

        mockMvc.perform(patch("/api/comments/5")
                        .header("Authorization", bearer(2L))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value(5));
        expectError(mockMvc.perform(patch("/api/comments/5")
                .header("Authorization", bearer(1L))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)), 403, "COMMENT-002");
        expectError(mockMvc.perform(patch("/api/comments/999")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)), 404, "COMMENT-001");
    }

    // ===== 삭제 =====

    @Test
    @DisplayName("작성자가 삭제하면 본문 없이 204, 남의 댓글은 403, 없는 댓글은 404예요")
    void delete_comment() throws Exception {
        willThrow(new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR))
                .given(commentService).delete(5L, 1L);
        willThrow(new Backend14thException(ErrorCode.COMMENT_NOT_FOUND))
                .given(commentService).delete(999L, 2L);

        mockMvc.perform(delete("/api/comments/5").header("Authorization", bearer(2L)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        then(commentService).should().delete(5L, 2L);

        expectError(mockMvc.perform(delete("/api/comments/5").header("Authorization", bearer(1L))),
                403, "COMMENT-002");
        expectError(mockMvc.perform(delete("/api/comments/999").header("Authorization", bearer(2L))),
                404, "COMMENT-001");
    }

    @Test
    @DisplayName("로그아웃한 토큰으로는 댓글을 작성할 수 없어요")
    void create_revokedToken() throws Exception {
        String token = bearer(2L);
        given(tokenRevocationService.isRevoked(token.substring("Bearer ".length()))).willReturn(true);

        expectError(mockMvc.perform(post("/api/study-posts/10/comments")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(BODY)), 401, "COMMON-004");
        then(commentService).should(never()).create(anyLong(), anyLong(), any());
    }

    // ===== 도우미 =====

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
