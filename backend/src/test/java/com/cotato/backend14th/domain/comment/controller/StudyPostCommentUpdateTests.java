package com.cotato.backend14th.domain.comment.controller;

import com.cotato.backend14th.JwtTestConfiguration;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:study-post-comment-update")
@AutoConfigureMockMvc
@Transactional
class StudyPostCommentUpdateTests {

    private static final String PATH = "/api/comments/{commentId}";
    private static final String ORIGINAL = "기존 댓글";
    private static final String UPDATED = "{\"content\":\"수정한 댓글\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager entityManager;
    @Autowired private UserRepository users;
    @Autowired private StudyPostRepository posts;
    @Autowired private JwtEncoder jwtEncoder;

    private Long postId;
    private Long commentId;
    private Long authorId;
    private Long postOwnerId;
    private String authorToken;

    @BeforeEach
    void setUpComment() {
        User owner = users.save(new User("owner@example.com", "test-hash", "홍길동", "길동이", 14, Part.BE));
        User author = users.save(new User("author@example.com", "test-hash", "김철수", "철수", 13, Part.FE));
        StudyPost post = posts.save(new StudyPost("스터디 모집", "같이 공부해요", owner));
        StudyPostComment comment = new StudyPostComment(ORIGINAL, post, author);
        entityManager.persist(comment);
        postId = post.getId();
        postOwnerId = owner.getId();
        commentId = comment.getId();
        authorId = author.getId();
        authorToken = token(authorId, Instant.now().plusSeconds(60));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void authorUpdatesFiveHundredCharactersAndPublicListShowsNewContent() throws Exception {
        String content = "글".repeat(500);
        update(commentId, authorToken, objectMapper.writeValueAsString(Map.of("content", content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.commentId").value(commentId.intValue()));
        assertStored(content);

        mockMvc.perform(get("/api/study-posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].commentId").value(commentId.intValue()))
                .andExpect(jsonPath("$.data.content[0].content").value(content));
    }

    @Test
    void authorCanUpdateExistingCommentOnClosedPost() throws Exception {
        posts.findById(postId).orElseThrow().update(null, null, StudyStatus.CLOSED);
        entityManager.flush();
        entityManager.clear();

        update(commentId, authorToken, UPDATED).andExpect(status().isOk());
        assertStored("수정한 댓글");
        assertThat(posts.findById(postId).orElseThrow().getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
    }

    @Test
    void postOwnerCannotUpdateAnotherUsersComment() throws Exception {
        expectError(update(commentId, token(postOwnerId, Instant.now().plusSeconds(60)), UPDATED), 403, "COMMENT-002");
        assertStored(ORIGINAL);
    }

    @Test
    void rejectsMissingTamperedExpiredAndRevokedTokens() throws Exception {
        int signatureStart = authorToken.lastIndexOf('.') + 1;
        char replacement = authorToken.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = authorToken.substring(0, signatureStart) + replacement + authorToken.substring(signatureStart + 1);
        expectError(update(commentId, null, UPDATED), 401, "COMMON-004");
        expectError(update(commentId, tampered, UPDATED), 401, "COMMON-004");
        expectError(update(commentId, token(authorId, Instant.now().minusSeconds(1)), UPDATED), 401, "COMMON-004");

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isNoContent());
        expectError(update(commentId, authorToken, UPDATED), 401, "COMMON-004");
        assertStored(ORIGINAL);
    }

    @Test
    void returnsNotFoundForMissingComment() throws Exception {
        expectError(update(Long.MAX_VALUE, authorToken, UPDATED), 404, "COMMENT-001");
    }

    @Test
    void rejectsFiveHundredAndOneCharacters() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "글".repeat(501)));
        expectError(update(commentId, authorToken, body), 400, "COMMON-003");
        assertStored(ORIGINAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"content\":\"\"}", "{\"content\":\" \"}", "{\"content\":null}", "{}", "{\"content\":42}"})
    void rejectsInvalidContent(String body) throws Exception {
        expectError(update(commentId, authorToken, body), 400, "COMMON-003");
        assertStored(ORIGINAL);
    }

    private void assertStored(String content) {
        entityManager.flush();
        entityManager.clear();
        StudyPostComment comment = entityManager.find(StudyPostComment.class, commentId);
        assertThat(comment.getContent()).isEqualTo(content);
        assertThat(comment.getAuthor().getId()).isEqualTo(authorId);
        assertThat(comment.getStudyPost().getId()).isEqualTo(postId);
    }

    private ResultActions update(Long id, String token, String body) throws Exception {
        var request = patch(PATH, id).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private void expectError(ResultActions result, int statusCode, String code) throws Exception {
        result.andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(code));
    }

    private String token(Long userId, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(userId.toString())
                .id(UUID.randomUUID().toString()).issuedAt(expiresAt.minusSeconds(3600)).expiresAt(expiresAt).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
