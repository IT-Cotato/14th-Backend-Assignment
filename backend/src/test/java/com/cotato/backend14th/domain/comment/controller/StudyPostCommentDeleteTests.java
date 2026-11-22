package com.cotato.backend14th.domain.comment.controller;

import com.cotato.backend14th.JwtTestConfiguration;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:study-post-comment-delete")
@AutoConfigureMockMvc
@Transactional
class StudyPostCommentDeleteTests {

    private static final String PATH = "/api/comments/{commentId}";
    private static final String ORIGINAL = "기존 댓글";

    @Autowired private MockMvc mockMvc;
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
    void authorDeletesOnlyTheTargetCommentAndPublicCountsDecrease() throws Exception {
        User owner = users.findById(postOwnerId).orElseThrow();
        StudyPost post = posts.findById(postId).orElseThrow();
        StudyPostComment sibling = new StudyPostComment("다른 댓글", post, owner);
        entityManager.persist(sibling);
        Long siblingId = sibling.getId();
        assertPublicCount(2);

        remove(commentId, authorToken)
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        flushAndClear();

        assertThat(entityManager.find(StudyPostComment.class, commentId)).isNull();
        assertThat(entityManager.find(StudyPostComment.class, siblingId).getContent()).isEqualTo("다른 댓글");
        assertThat(posts.existsById(postId)).isTrue();
        mockMvc.perform(get("/api/study-posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].commentId").value(siblingId.intValue()));
        assertPublicCount(1);
    }

    @Test
    void authorCanDeleteExistingCommentOnClosedPost() throws Exception {
        posts.findById(postId).orElseThrow().update(null, null, StudyStatus.CLOSED);
        flushAndClear();

        remove(commentId, authorToken).andExpect(status().isNoContent());
        flushAndClear();
        assertThat(entityManager.find(StudyPostComment.class, commentId)).isNull();
        assertThat(posts.findById(postId).orElseThrow().getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
    }

    @Test
    void postOwnerCannotDeleteAnotherUsersComment() throws Exception {
        expectError(remove(commentId, token(postOwnerId, Instant.now().plusSeconds(60))), 403, "COMMENT-002");
        assertCommentRemains();
    }

    @Test
    void rejectsMissingTamperedExpiredAndRevokedTokens() throws Exception {
        int signatureStart = authorToken.lastIndexOf('.') + 1;
        char replacement = authorToken.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = authorToken.substring(0, signatureStart) + replacement + authorToken.substring(signatureStart + 1);
        expectError(remove(commentId, null), 401, "COMMON-004");
        expectError(remove(commentId, tampered), 401, "COMMON-004");
        expectError(remove(commentId, token(authorId, Instant.now().minusSeconds(1))), 401, "COMMON-004");

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isNoContent());
        expectError(remove(commentId, authorToken), 401, "COMMON-004");
        mockMvc.perform(get("/api/study-posts/{postId}/comments", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].isAuthor").value(false));
        assertCommentRemains();
    }

    @Test
    void returnsNotFoundForMissingComment() throws Exception {
        expectError(remove(Long.MAX_VALUE, authorToken), 404, "COMMENT-001");
    }

    private void assertPublicCount(int count) throws Exception {
        mockMvc.perform(get("/api/study-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].postId").value(postId.intValue()))
                .andExpect(jsonPath("$.data.content[0].commentCount").value(count));
    }

    private void assertCommentRemains() {
        flushAndClear();
        assertThat(entityManager.find(StudyPostComment.class, commentId)).isNotNull();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private ResultActions remove(Long id, String token) throws Exception {
        var request = delete(PATH, id);
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
