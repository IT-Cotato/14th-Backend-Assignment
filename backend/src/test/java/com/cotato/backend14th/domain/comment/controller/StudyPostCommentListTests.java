package com.cotato.backend14th.domain.comment.controller;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:study-post-comment-list")
@AutoConfigureMockMvc
@Transactional
class StudyPostCommentListTests {

    private static final String PATH = "/api/study-posts/{postId}/comments";
    private static final LocalDateTime OLDEST_TIME = LocalDateTime.of(2026, 11, 8, 13, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private UserRepository users;
    @Autowired private StudyPostRepository posts;
    @Autowired private JwtEncoder jwtEncoder;

    private Long postId;
    private Long emptyPostId;
    private Long firstAuthorId;
    private Long unrelatedUserId;
    private int oldestId;
    private int firstTieId;
    private int secondTieId;

    @BeforeEach
    void setUpComments() {
        User firstAuthor = users.save(new User("first@example.com", "test-hash", "김철수", "철수", 13, Part.FE));
        User secondAuthor = users.save(new User("second@example.com", "test-hash", "홍길동", "길동이", 14, Part.BE));
        firstAuthorId = firstAuthor.getId();
        unrelatedUserId = users.save(new User("viewer@example.com", "test-hash", "이민수", "민수", 14, Part.PM)).getId();
        StudyPost post = posts.save(new StudyPost("스터디 모집", "함께 공부해요", secondAuthor));
        postId = post.getId();
        emptyPostId = posts.save(new StudyPost("빈 게시글", "아직 댓글이 없어요", secondAuthor)).getId();
        StudyPost otherPost = posts.save(new StudyPost("다른 게시글", "목록에 포함되지 않아요", secondAuthor));

        firstTieId = saveComment(post, firstAuthor, "같은 시각 첫 댓글", OLDEST_TIME.plusMinutes(1));
        secondTieId = saveComment(post, secondAuthor, "같은 시각 두 번째 댓글", OLDEST_TIME.plusMinutes(1));
        oldestId = saveComment(post, firstAuthor, "가장 오래된 댓글", OLDEST_TIME);
        saveComment(otherPost, secondAuthor, "다른 게시글의 댓글", OLDEST_TIME);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void returnsPublicCommentsOldestFirstWithStableOrderingAndExactFields() throws Exception {
        mockMvc.perform(get(PATH, postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.data", aMapWithSize(5)))
                .andExpect(jsonPath("$.data.content[*].commentId", contains(oldestId, firstTieId, secondTieId)))
                .andExpect(jsonPath("$.data.content[*].isAuthor", contains(false, false, false)))
                .andExpect(jsonPath("$.data.content[0]", aMapWithSize(5)))
                .andExpect(jsonPath("$.data.content[0].content").value("가장 오래된 댓글"))
                .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-11-08T13:00:00"))
                .andExpect(jsonPath("$.data.content[0].author", aMapWithSize(3)))
                .andExpect(jsonPath("$.data.content[0].author.nickname").value("철수"))
                .andExpect(jsonPath("$.data.content[0].author.generation").value(13))
                .andExpect(jsonPath("$.data.content[0].author.part").value("FE"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void marksOnlyCommentsByTheAuthenticatedViewer() throws Exception {
        expectAuthorFlags(list(token(firstAuthorId, Instant.now().plusSeconds(60))), true, true, false);
        expectAuthorFlags(list(token(unrelatedUserId, Instant.now().plusSeconds(60))), false, false, false);
    }

    @Test
    void treatsTamperedExpiredAndRevokedTokensAsAnonymous() throws Exception {
        Instant now = Instant.now();
        String token = token(firstAuthorId, now.plusSeconds(60));
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
        expectAuthorFlags(list(tampered), false, false, false);
        expectAuthorFlags(list(token(firstAuthorId, now.minusSeconds(1))), false, false, false);

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        expectAuthorFlags(list(token), false, false, false);
    }

    @Test
    void returnsTwentyCommentsOnTheDefaultPageWhenMoreExist() throws Exception {
        StudyPost post = posts.findById(emptyPostId).orElseThrow();
        User author = users.findById(firstAuthorId).orElseThrow();
        for (int i = 0; i < 21; i++) {
            entityManager.persist(new StudyPostComment("댓글 " + i, post, author));
        }
        entityManager.flush();

        mockMvc.perform(get(PATH, emptyPostId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(20)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(21))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void paginatesAcrossTiedTimestampsAndReturnsTotals() throws Exception {
        mockMvc.perform(get(PATH, postId).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].commentId", contains(oldestId, firstTieId)))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
        mockMvc.perform(get(PATH, postId).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].commentId", contains(secondTieId)))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void returnsEmptyContentForExistingPostWithoutComments() throws Exception {
        mockMvc.perform(get(PATH, emptyPostId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0));
    }

    @Test
    void returnsNotFoundForMissingPost() throws Exception {
        mockMvc.perform(get(PATH, Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY-001"));
    }

    @ParameterizedTest
    @CsvSource({"page, -1", "page, 1.5", "page, 2147483647", "page, ''",
            "size, 0", "size, -1", "size, x", "size, ''"})
    void rejectsInvalidPagination(String parameter, String value) throws Exception {
        mockMvc.perform(get(PATH, postId).param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-003"));
    }

    private int saveComment(StudyPost post, User author, String content, LocalDateTime createdAt) {
        StudyPostComment comment = new StudyPostComment(content, post, author);
        entityManager.persist(comment);
        entityManager.createQuery("update StudyPostComment c set c.createdAt = :time where c.id = :id")
                .setParameter("time", createdAt)
                .setParameter("id", comment.getId())
                .executeUpdate();
        return comment.getId().intValue();
    }

    private ResultActions list(String token) throws Exception {
        return mockMvc.perform(get(PATH, postId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private void expectAuthorFlags(ResultActions response, Boolean... flags) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].isAuthor", contains(flags)));
    }

    private String token(Long userId, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(userId.toString())
                .id(UUID.randomUUID().toString()).issuedAt(expiresAt.minusSeconds(3600)).expiresAt(expiresAt).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
