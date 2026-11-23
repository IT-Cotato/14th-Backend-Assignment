package com.cotato.backend14th.domain.studypost.controller;

import com.cotato.backend14th.JwtTestConfiguration;
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
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:study-post-update")
@AutoConfigureMockMvc
@Transactional
class StudyPostUpdateTests {

    private static final String PATH = "/api/study-posts/{postId}";
    private static final String UPDATE_TITLE = """
            {"title":"변경된 제목"}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private StudyPostRepository posts;
    @Autowired private EntityManager entityManager;
    @Autowired private JwtEncoder jwtEncoder;

    private Long postId;
    private Long authorId;
    private String authorToken;

    @BeforeEach
    void setUpPost() {
        User author = users.save(new User("author@example.com", "test-hash", "홍길동", "길동이", 14, Part.BE));
        authorId = author.getId();
        postId = posts.save(new StudyPost("원래 제목", "원래 내용", author)).getId();
        authorToken = token(authorId, Instant.now().plusSeconds(60));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void updatesAndPersistsAllFieldsAtAllowedBoundaries() throws Exception {
        String title = "제".repeat(50);
        String content = "글".repeat(2000);
        update(postId, authorToken, objectMapper.writeValueAsString(
                Map.of("title", title, "content", content, "studyStatus", "CLOSED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.postId").value(postId.intValue()));
        assertStored(title, content, StudyStatus.CLOSED);
    }

    @Test
    void preservesOmittedFieldsAndPermitsBothStatusTransitions() throws Exception {
        update(postId, authorToken, UPDATE_TITLE).andExpect(status().isOk());
        assertStored("변경된 제목", "원래 내용", StudyStatus.RECRUITING);

        update(postId, authorToken, "{\"studyStatus\":\"CLOSED\"}").andExpect(status().isOk());
        assertStored("변경된 제목", "원래 내용", StudyStatus.CLOSED);

        update(postId, authorToken, "{\"studyStatus\":\"RECRUITING\"}").andExpect(status().isOk());
        update(postId, authorToken, "{}").andExpect(status().isOk());
        assertStored("변경된 제목", "원래 내용", StudyStatus.RECRUITING);
    }

    @Test
    void returnsForbiddenForAnotherUser() throws Exception {
        User other = users.save(new User("other@example.com", "test-hash", "김철수", "철수", 13, Part.FE));
        expectError(update(postId, token(other.getId(), Instant.now().plusSeconds(60)), UPDATE_TITLE),
                403, "STUDY-002");
        assertUnchanged();
    }

    @Test
    void rejectsMissingTamperedExpiredAndRevokedTokensWhileDetailStaysPublic() throws Exception {
        int signatureStart = authorToken.lastIndexOf('.') + 1;
        char replacement = authorToken.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = authorToken.substring(0, signatureStart) + replacement
                + authorToken.substring(signatureStart + 1);

        expectError(update(postId, null, UPDATE_TITLE), 401, "COMMON-004");
        expectError(update(postId, tampered, UPDATE_TITLE), 401, "COMMON-004");
        expectError(update(postId, token(authorId, Instant.now().minusSeconds(1)), UPDATE_TITLE), 401, "COMMON-004");

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isNoContent());
        expectError(update(postId, authorToken, UPDATE_TITLE), 401, "COMMON-004");
        mockMvc.perform(get(PATH, postId).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAuthor").value(false));
        assertUnchanged();
    }

    @Test
    void returnsNotFoundForMissingPost() throws Exception {
        expectError(update(Long.MAX_VALUE, authorToken, UPDATE_TITLE), 404, "STUDY-001");
    }

    @Test
    void rejectsOversizedTitleAndContent() throws Exception {
        expectError(update(postId, authorToken, objectMapper.writeValueAsString(Map.of("title", "제".repeat(51)))),
                400, "COMMON-003");
        expectError(update(postId, authorToken, objectMapper.writeValueAsString(Map.of("content", "글".repeat(2001)))),
                400, "COMMON-003");
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"title\":null}", "{\"content\":null}", "{\"studyStatus\":null}",
            "{\"title\":\"\"}", "{\"content\":\" \"}", "{\"studyStatus\":\"OPEN\"}",
            "{\"studyStatus\":1}", "{\"title\":123}"})
    void rejectsInvalidFields(String body) throws Exception {
        expectError(update(postId, authorToken, body), 400, "COMMON-003");
        assertUnchanged();
    }

    private void assertUnchanged() {
        assertStored("원래 제목", "원래 내용", StudyStatus.RECRUITING);
    }

    private void assertStored(String title, String content, StudyStatus studyStatus) {
        entityManager.flush();
        entityManager.clear();
        StudyPost post = posts.findById(postId).orElseThrow();
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getStudyStatus()).isEqualTo(studyStatus);
        assertThat(post.getAuthor().getId()).isEqualTo(authorId);
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
                .andExpect(jsonPath("$.status").value(statusCode))
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
