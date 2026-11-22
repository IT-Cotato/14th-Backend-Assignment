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
import org.hibernate.Hibernate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:study-post")
@AutoConfigureMockMvc
@Transactional
class StudyPostControllerTests {

    private static final String PATH = "/api/study-posts";
    private static final String VALID_BODY = """
            {"title":"스터디 모집","content":"함께 공부해요"}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private StudyPostRepository posts;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private EntityManager entityManager;

    private Long authorId;

    @BeforeEach
    void createAuthor() {
        authorId = users.save(new User("author@example.com", "unused-test-hash", "홍길동", "길동이", 14, Part.BE)).getId();
    }

    @ParameterizedTest(name = "saves content of length {0}")
    @ValueSource(ints = {1, 2000})
    void createsRecruitingPostForAuthenticatedUser(int contentLength) throws Exception {
        String title = "제".repeat(50);
        String content = "글".repeat(contentLength);
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title, "content", content, "authorId", -1, "studyStatus", "CLOSED"));
        String response = create(body, token(authorId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andReturn().getResponse().getContentAsString();

        long postId = objectMapper.readTree(response).at("/data/postId").asLong();
        assertThat(postId).isPositive();
        entityManager.flush();
        entityManager.clear();
        StudyPost post = posts.findById(postId).orElseThrow();
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
        assertThat(post.getCreatedAt()).isNotNull();
        assertThat(post.getUpdatedAt()).isNotNull();
        assertThat(Hibernate.isInitialized(post.getAuthor())).isFalse();
        assertThat(post.getAuthor().getId()).isEqualTo(authorId);
    }

    @ParameterizedTest(name = "rejects title of length {0}")
    @ValueSource(ints = {0, 51})
    void rejectsInvalidTitleLength(int titleLength) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", "제".repeat(titleLength), "content", "내용"));
        expectInvalidInput(body);
    }

    @ParameterizedTest(name = "rejects content of length {0}")
    @ValueSource(ints = {0, 2001})
    void rejectsInvalidContentLength(int contentLength) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "제목", "content", "글".repeat(contentLength)));
        expectInvalidInput(body);
    }

    @Test
    void rejectsMalformedAndNonStringInput() throws Exception {
        expectInvalidInput("{");
        expectInvalidInput("""
                {"title":123,"content":"내용"}
                """);
    }

    @Test
    void rejectsMissingInvalidAndUnknownUserTokens() throws Exception {
        expectUnauthorized(create(VALID_BODY, null));
        expectUnauthorized(create(VALID_BODY, "invalid-token"));
        expectUnauthorized(create(VALID_BODY, token(Long.MAX_VALUE)));
    }

    @Test
    void rejectsRevokedToken() throws Exception {
        String token = token(authorId);
        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        expectUnauthorized(create(VALID_BODY, token));
    }

    private ResultActions create(String body, String token) throws Exception {
        var request = post(PATH).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private String token(Long userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private void expectInvalidInput(String body) throws Exception {
        create(body, token(authorId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-003"));
        assertThat(posts.count()).isZero();
    }

    private void expectUnauthorized(ResultActions response) throws Exception {
        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON-004"));
        assertThat(posts.count()).isZero();
    }
}
