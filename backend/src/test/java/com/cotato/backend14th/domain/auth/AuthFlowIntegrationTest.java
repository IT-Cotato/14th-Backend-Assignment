package com.cotato.backend14th.domain.auth;

import com.cotato.backend14th.domain.auth.entity.RevokedToken;
import com.cotato.backend14th.domain.auth.repository.RevokedTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 DB와 JWT로 회원가입 → 로그인 → 내 정보 조회 흐름을 확인해요.
// 테스트가 끝나면 @Transactional로 저장한 회원을 롤백해요.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @DisplayName("회원가입 후 로그인해서 받은 JWT로 내 정보를 조회해요")
    void signupLoginAndGetMyInfo() throws Exception {
        String email = "it-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";

        String signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1234","name":"홍길동",
                                 "nickname":"길동이","generation":14,"part":"BE"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(signupResult).path("data").path("userId").asLong();

        // 중복 이메일은 409를 반환해요
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1234","name":"홍길동",
                                 "nickname":"길동이","generation":14,"part":"BE"}
                                """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER-001"));

        // 비밀번호가 틀리면 401을 반환해요
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong-password"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER-002"));

        String loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1234"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        JsonNode loginData = objectMapper.readTree(loginResult).path("data");
        String accessToken = loginData.path("accessToken").asText();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value("길동이"))
                .andExpect(jsonPath("$.data.part").value("BE"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON-004"));

        // 토큰 없이 로그아웃하면 401이에요
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON-004"));

        // 정상 로그아웃은 본문 없이 204예요
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // 로그아웃한 토큰으로는 내 정보 조회와 재로그아웃이 모두 401이에요
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON-004"));
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        // 무효화 테이블에는 토큰 원문이 아니라 해시만 저장돼요
        assertThat(revokedTokenRepository.findAll())
                .extracting(RevokedToken::getTokenHash)
                .doesNotContain(accessToken)
                .allMatch(hash -> hash.matches("[0-9a-f]{64}"));

        // 다시 로그인하면 새 토큰으로 정상 조회돼요
        String reLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1234"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newToken = objectMapper.readTree(reLoginResult).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    @DisplayName("기존 /books, /hello API는 토큰 없이 그대로 동작해요")
    void legacyApisArePublic() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk());

        String createResult = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"클린 코드","author":"로버트 C. 마틴"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("클린 코드"))
                .andReturn().getResponse().getContentAsString();
        long bookId = objectMapper.readTree(createResult).path("data").path("id").asLong();

        mockMvc.perform(get("/books/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.author").value("로버트 C. 마틴"));

        // 없는 책은 기존처럼 404 BOOK-001을 반환해요
        mockMvc.perform(get("/books/" + Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK-001"));
    }
}
