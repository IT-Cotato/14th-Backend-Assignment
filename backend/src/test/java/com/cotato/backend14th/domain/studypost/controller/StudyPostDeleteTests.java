package com.cotato.backend14th.domain.studypost.controller;

import com.cotato.backend14th.JwtTestConfiguration;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:study-post-delete")
@AutoConfigureMockMvc
@Transactional
class StudyPostDeleteTests {

    private static final String PATH = "/api/study-posts/{postId}";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private StudyPostRepository posts;
    @Autowired private EntityManager entityManager;
    @Autowired private JwtEncoder jwtEncoder;

    private User author;
    private StudyPost studyPost;
    private String authorToken;

    @BeforeEach
    void setUpPost() {
        author = users.save(new User("author@example.com", "test-hash", "홍길동", "길동이", 14, Part.BE));
        studyPost = posts.save(new StudyPost("스터디 모집", "같이 공부해요", author));
        authorToken = token(author.getId(), Instant.now().plusSeconds(60));
    }

    @Test
    void authorDeletesPostAndDetailReturnsNotFound() throws Exception {
        remove(studyPost.getId(), authorToken)
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        flushAndClear();

        assertThat(posts.existsById(studyPost.getId())).isFalse();
        mockMvc.perform(get(PATH, studyPost.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY-001"));
    }

    @Test
    void deletesOnlyCommentsOfTargetPost() throws Exception {
        StudyPost otherPost = posts.save(new StudyPost("다른 모집글", "다른 내용", author));
        entityManager.persist(new StudyPostComment("첫 댓글", studyPost, author));
        entityManager.persist(new StudyPostComment("두 번째 댓글", studyPost, author));
        entityManager.persist(new StudyPostComment("남아야 하는 댓글", otherPost, author));
        flushAndClear();

        remove(studyPost.getId(), authorToken).andExpect(status().isNoContent());
        flushAndClear();

        assertThat(posts.existsById(studyPost.getId())).isFalse();
        assertThat(countComments(studyPost.getId())).isZero();
        assertThat(posts.existsById(otherPost.getId())).isTrue();
        assertThat(countComments(otherPost.getId())).isEqualTo(1);
    }

    @Test
    void anotherUserCannotDeletePostOrComments() throws Exception {
        User other = users.save(new User("other@example.com", "test-hash", "김철수", "철수", 13, Part.FE));
        entityManager.persist(new StudyPostComment("남아야 하는 댓글", studyPost, other));

        expectError(remove(studyPost.getId(), token(other.getId(), Instant.now().plusSeconds(60))), 403, "STUDY-002");
        flushAndClear();
        assertThat(posts.existsById(studyPost.getId())).isTrue();
        assertThat(countComments(studyPost.getId())).isEqualTo(1);
    }

    @Test
    void rejectsMissingTamperedExpiredAndRevokedTokens() throws Exception {
        int signatureStart = authorToken.lastIndexOf('.') + 1;
        char replacement = authorToken.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = authorToken.substring(0, signatureStart) + replacement
                + authorToken.substring(signatureStart + 1);

        expectError(remove(studyPost.getId(), null), 401, "COMMON-004");
        expectError(remove(studyPost.getId(), tampered), 401, "COMMON-004");
        expectError(remove(studyPost.getId(), token(author.getId(), Instant.now().minusSeconds(1))), 401, "COMMON-004");

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isNoContent());
        expectError(remove(studyPost.getId(), authorToken), 401, "COMMON-004");
        mockMvc.perform(get(PATH, studyPost.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAuthor").value(false));
    }

    @Test
    void returnsNotFoundForMissingPost() throws Exception {
        expectError(remove(Long.MAX_VALUE, authorToken), 404, "STUDY-001");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private long countComments(Long postId) {
        return entityManager.createQuery(
                        "select count(c.id) from StudyPostComment c where c.studyPost.id = :postId", Long.class)
                .setParameter("postId", postId)
                .getSingleResult();
    }

    private ResultActions remove(Long postId, String token) throws Exception {
        var request = delete(PATH, postId);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private void expectError(ResultActions response, int statusCode, String code) throws Exception {
        response.andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(code));
    }

    private String token(Long userId, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
