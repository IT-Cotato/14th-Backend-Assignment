package com.cotato.backend14th.domain.studypost.controller;

import com.cotato.backend14th.JwtTestConfiguration;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:study-post-detail")
@AutoConfigureMockMvc
@Transactional
class StudyPostDetailTests {

    private static final String PATH = "/api/study-posts/{postId}";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private StudyPostRepository posts;
    @Autowired private JwtEncoder jwtEncoder;

    private User author;
    private StudyPost studyPost;

    @BeforeEach
    void setUpPost() {
        author = users.save(new User("author@example.com", "test-hash", "홍길동", "길동이", 14, Part.BE));
        studyPost = posts.save(new StudyPost("알고리즘 스터디 모집", "함께 공부해요", author));
    }

    @Test
    void returnsPublicDetailWithSpecifiedFields() throws Exception {
        mockMvc.perform(get(PATH, studyPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.data", aMapWithSize(8)))
                .andExpect(jsonPath("$.data.postId").value(studyPost.getId().intValue()))
                .andExpect(jsonPath("$.data.title").value("알고리즘 스터디 모집"))
                .andExpect(jsonPath("$.data.content").value("함께 공부해요"))
                .andExpect(jsonPath("$.data.studyStatus").value("RECRUITING"))
                .andExpect(jsonPath("$.data.author", aMapWithSize(3)))
                .andExpect(jsonPath("$.data.author.nickname").value("길동이"))
                .andExpect(jsonPath("$.data.author.generation").value(14))
                .andExpect(jsonPath("$.data.author.part").value("BE"))
                .andExpect(jsonPath("$.data.isAuthor").value(false))
                .andExpect(jsonPath("$.data.createdAt").isString())
                .andExpect(jsonPath("$.data.updatedAt").isString());
    }

    @Test
    void onlyMarksAuthorForTheAuthorsToken() throws Exception {
        User other = users.save(new User("other@example.com", "test-hash", "김철수", "철수", 13, Part.FE));
        expectAuthorFlag(token(author.getId(), Instant.now().plusSeconds(60)), true);
        expectAuthorFlag(token(other.getId(), Instant.now().plusSeconds(60)), false);
    }

    @Test
    void treatsInvalidExpiredAndRevokedTokensAsAnonymous() throws Exception {
        Instant now = Instant.now();
        String validToken = token(author.getId(), now.plusSeconds(60));
        int signatureStart = validToken.lastIndexOf('.') + 1;
        char replacement = validToken.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tamperedToken = validToken.substring(0, signatureStart) + replacement
                + validToken.substring(signatureStart + 1);

        expectAuthorFlag("malformed token", false);
        expectAuthorFlag(tamperedToken, false);
        expectAuthorFlag(token(author.getId(), now.minusSeconds(1)), false);
        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNoContent());
        expectAuthorFlag(validToken, false);
    }

    @Test
    void returnsCommonNotFoundResponseForMissingPost() throws Exception {
        long missingId = Long.MAX_VALUE;
        mockMvc.perform(get(PATH, missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("STUDY-001"))
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.path").value("/api/study-posts/" + missingId));
    }

    private void expectAuthorFlag(String token, boolean authorFlag) throws Exception {
        mockMvc.perform(get(PATH, studyPost.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAuthor").value(authorFlag));
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
