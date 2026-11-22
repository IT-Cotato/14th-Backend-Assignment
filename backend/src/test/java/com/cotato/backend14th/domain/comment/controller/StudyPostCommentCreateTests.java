package com.cotato.backend14th.domain.comment.controller;

import com.cotato.backend14th.JwtTestConfiguration;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
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
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:study-post-comment-create")
@AutoConfigureMockMvc
@Transactional
class StudyPostCommentCreateTests {

    private static final String PATH = "/api/study-posts/{postId}/comments";
    private static final String VALID_BODY = "{\"content\":\"참여하고 싶어요\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager entityManager;
    @Autowired private UserRepository users;
    @Autowired private StudyPostRepository posts;
    @Autowired private JwtEncoder jwtEncoder;

    private Long postId;
    private Long ownerId;
    private Long commenterId;
    private String commenterToken;

    @BeforeEach
    void setUpPost() {
        User owner = users.save(new User("owner@example.com", "test-hash", "홍길동", "길동이", 14, Part.BE));
        ownerId = owner.getId();
        commenterId = users.save(new User("commenter@example.com", "test-hash", "김철수", "철수", 13, Part.FE)).getId();
        postId = posts.save(new StudyPost("스터디 모집", "같이 공부해요", owner)).getId();
        commenterToken = token(commenterId, Instant.now().plusSeconds(60));
    }

    @Test
    void storesMultipleCommentsByAuthenticatedAuthorAndUpdatesPublicCount() throws Exception {
        expectPublicCommentCount(0);
        String content = "글".repeat(500);
        String body = objectMapper.writeValueAsString(Map.of("content", content, "authorId", ownerId));
        long firstId = createdId(create(postId, commenterToken, body));
        long secondId = createdId(create(postId, commenterToken, "{\"content\":\"글\"}"));
        assertThat(firstId).isNotEqualTo(secondId);

        entityManager.flush();
        entityManager.clear();
        StudyPostComment first = entityManager.find(StudyPostComment.class, firstId);
        assertThat(first.getContent()).isEqualTo(content);
        assertThat(first.getAuthor().getId()).isEqualTo(commenterId);
        assertThat(first.getStudyPost().getId()).isEqualTo(postId);
        assertThat(first.getCreatedAt()).isNotNull();
        assertThat(first.getUpdatedAt()).isNotNull();
        assertThat(entityManager.find(StudyPostComment.class, secondId).getAuthor().getId()).isEqualTo(commenterId);
        expectPublicCommentCount(2);
    }

    @Test
    void rejectsCommentAfterPostIsClosedThroughPatch() throws Exception {
        mockMvc.perform(patch("/api/study-posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(ownerId, Instant.now().plusSeconds(60)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"studyStatus\":\"CLOSED\"}"))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        expectError(create(postId, commenterToken, VALID_BODY), 409, "STUDY-003");
        expectPublicCommentCount(0);
    }

    @Test
    void rejectsMissingPost() throws Exception {
        expectError(create(Long.MAX_VALUE, commenterToken, VALID_BODY), 404, "STUDY-001");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 501})
    void rejectsInvalidContentLength(int length) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "글".repeat(length)));
        expectError(create(postId, commenterToken, body), 400, "COMMON-003");
        expectPublicCommentCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"content\":null}", "{\"content\":42}", "{\"content\":\" \"}"})
    void rejectsMissingBlankOrNonTextContent(String body) throws Exception {
        expectError(create(postId, commenterToken, body), 400, "COMMON-003");
    }

    @Test
    void rejectsMissingTamperedExpiredAndRevokedTokens() throws Exception {
        int signatureStart = commenterToken.lastIndexOf('.') + 1;
        char replacement = commenterToken.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = commenterToken.substring(0, signatureStart) + replacement
                + commenterToken.substring(signatureStart + 1);
        expectError(create(postId, null, VALID_BODY), 401, "COMMON-004");
        expectError(create(postId, tampered, VALID_BODY), 401, "COMMON-004");
        expectError(create(postId, token(commenterId, Instant.now().minusSeconds(1)), VALID_BODY), 401, "COMMON-004");

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + commenterToken))
                .andExpect(status().isNoContent());
        expectError(create(postId, commenterToken, VALID_BODY), 401, "COMMON-004");
        expectPublicCommentCount(0);
    }

    private ResultActions create(Long id, String token, String body) throws Exception {
        var request = post(PATH, id).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private long createdId(ResultActions response) throws Exception {
        String body = response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).at("/data/commentId").asLong();
        assertThat(id).isPositive();
        return id;
    }

    private void expectPublicCommentCount(int count) throws Exception {
        mockMvc.perform(get("/api/study-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].postId").value(postId.intValue()))
                .andExpect(jsonPath("$.data.content[0].commentCount").value(count));
    }

    private void expectError(ResultActions response, int statusCode, String code) throws Exception {
        response.andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(code));
    }

    private String token(Long userId, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
