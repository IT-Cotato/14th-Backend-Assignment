package com.cotato.backend14th.domain.studypost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 MySQL로 모집글 CRUD, 검색·필터·페이징, 작성자 권한, N+1 여부를 확인해요.
// 다른 데이터와 섞이지 않도록 테스트마다 고유한 키워드(tag)를 제목에 넣고 그 키워드로만 검색해요.
// 테스트가 끝나면 @Transactional로 모두 롤백해요.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Transactional
class StudyPostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String tag;
    private String beToken;
    private String feToken;

    @BeforeEach
    void setUp() throws Exception {
        tag = "it" + UUID.randomUUID().toString().substring(0, 8);
        beToken = signupAndLogin("be", "BE");
        feToken = signupAndLogin("fe", "FE");
    }

    @Test
    @DisplayName("작성 → 상세 → 부분 수정 → 삭제 흐름과 작성자 권한을 확인해요")
    void crudAndAuthorization() throws Exception {
        long postId = createPost(beToken, tag + " 알고리즘 스터디", "주 2회 백준 문제를 풀어요.");

        // 상세: 작성자 true, 다른 사용자 false, 비로그인 false
        mockMvc.perform(withToken(get("/api/study-posts/" + postId), beToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(tag + " 알고리즘 스터디"))
                .andExpect(jsonPath("$.data.content").value("주 2회 백준 문제를 풀어요."))
                .andExpect(jsonPath("$.data.studyStatus").value("RECRUITING"))
                .andExpect(jsonPath("$.data.author.part").value("BE"))
                .andExpect(jsonPath("$.data.isAuthor").value(true))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
        mockMvc.perform(withToken(get("/api/study-posts/" + postId), feToken))
                .andExpect(jsonPath("$.data.isAuthor").value(false));
        mockMvc.perform(get("/api/study-posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAuthor").value(false));

        // 다른 사용자는 수정·삭제할 수 없어요 (403)
        mockMvc.perform(withToken(patch("/api/study-posts/" + postId), feToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"가로채기\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STUDY-002"));
        mockMvc.perform(withToken(delete("/api/study-posts/" + postId), feToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STUDY-002"));

        // 작성자는 상태만 바꿀 수 있고, 나머지 필드는 그대로예요
        mockMvc.perform(withToken(patch("/api/study-posts/" + postId), beToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studyStatus\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(postId));
        mockMvc.perform(get("/api/study-posts/" + postId))
                .andExpect(jsonPath("$.data.title").value(tag + " 알고리즘 스터디"))
                .andExpect(jsonPath("$.data.content").value("주 2회 백준 문제를 풀어요."))
                .andExpect(jsonPath("$.data.studyStatus").value("CLOSED"));

        // 제목만 바꿔요
        mockMvc.perform(withToken(patch("/api/study-posts/" + postId), beToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + tag + " 수정된 제목\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/study-posts/" + postId))
                .andExpect(jsonPath("$.data.title").value(tag + " 수정된 제목"))
                .andExpect(jsonPath("$.data.studyStatus").value("CLOSED"));

        // 작성자가 삭제하면 204이고, 이후 조회·수정·삭제는 404예요
        mockMvc.perform(withToken(delete("/api/study-posts/" + postId), beToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/study-posts/" + postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY-001"));
        mockMvc.perform(withToken(delete("/api/study-posts/" + postId), beToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("로그아웃한 토큰으로는 모집글을 작성할 수 없어요")
    void create_withLoggedOutToken() throws Exception {
        mockMvc.perform(withToken(post("/api/auth/logout"), beToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(withToken(post("/api/study-posts"), beToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON-004"));
    }

    @Test
    @DisplayName("키워드·상태·파트 필터와 페이징, 최신순 정렬이 동작해요")
    void searchFilterPaging() throws Exception {
        long p1 = createPost(beToken, tag + " 알고리즘 기초", "백준 브론즈");
        long p2 = createPost(feToken, tag + " 리액트 스터디", "프론트엔드 알고리즘도 조금");
        long p3 = createPost(beToken, tag + " 스프링 스터디", "JPA 공부");
        long p4 = createPost(feToken, tag + " 디자인 패턴", "GoF");
        // p3은 모집 마감으로 바꿔요
        mockMvc.perform(withToken(patch("/api/study-posts/" + p3), beToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studyStatus\":\"CLOSED\"}"))
                .andExpect(status().isOk());

        // 태그로 전체 조회: 최신순(p4, p3, p2, p1)
        JsonNode all = list("keyword", tag);
        assertThat(postIds(all)).containsExactly(p4, p3, p2, p1);
        assertThat(all.path("totalElements").asLong()).isEqualTo(4);
        assertThat(all.path("content").get(0).path("author").path("part").asText()).isEqualTo("FE");
        assertThat(all.path("content").get(0).path("commentCount").asLong()).isZero();
        assertThat(all.path("content").get(0).has("createdAt")).isTrue();

        // 키워드는 제목과 내용 모두에서 찾아요 (p1 제목, p2 내용)
        assertThat(postIds(list("keyword", tag + " 알고리즘"))).containsExactly(p1);
        assertThat(postIds(listByKeywordAnd("알고리즘"))).containsExactlyInAnyOrder(p1, p2);

        // 상태 필터
        assertThat(postIds(list("keyword", tag, "status", "CLOSED"))).containsExactly(p3);
        assertThat(postIds(list("keyword", tag, "status", "RECRUITING"))).containsExactly(p4, p2, p1);

        // 작성자 파트 필터
        assertThat(postIds(list("keyword", tag, "part", "BE"))).containsExactly(p3, p1);
        assertThat(postIds(list("keyword", tag, "part", "FE"))).containsExactly(p4, p2);

        // 여러 조건은 모두 만족해야 해요
        assertThat(postIds(list("keyword", tag, "status", "RECRUITING", "part", "BE"))).containsExactly(p1);

        // 페이징: size=3이면 2페이지예요
        JsonNode page0 = list("keyword", tag, "page", "0", "size", "3");
        JsonNode page1 = list("keyword", tag, "page", "1", "size", "3");
        assertThat(postIds(page0)).containsExactly(p4, p3, p2);
        assertThat(postIds(page1)).containsExactly(p1);
        assertThat(page1.path("page").asInt()).isEqualTo(1);
        assertThat(page1.path("size").asInt()).isEqualTo(3);
        assertThat(page1.path("totalElements").asLong()).isEqualTo(4);
        assertThat(page1.path("totalPages").asInt()).isEqualTo(2);

        // 결과가 없으면 빈 목록이에요
        JsonNode none = list("keyword", tag + "없는검색어");
        assertThat(none.path("content")).isEmpty();
        assertThat(none.path("totalElements").asLong()).isZero();
    }

    @Test
    @DisplayName("작성자가 여러 명이어도 목록 조회는 쿼리 3개(목록+개수+댓글 수) 이내예요 (N+1 없음)")
    void list_noNPlusOne() throws Exception {
        for (int i = 0; i < 3; i++) {
            createPost(beToken, tag + " BE 글 " + i, "내용");
            createPost(feToken, tag + " FE 글 " + i, "내용");
        }
        // 영속성 컨텍스트를 비워서 작성자를 DB에서 새로 읽게 해요
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        JsonNode page = list("keyword", tag, "size", "4");

        assertThat(page.path("content")).hasSize(4);
        assertThat(page.path("totalElements").asLong()).isEqualTo(6);
        // 목록 1 + 전체 개수 1 + 댓글 수(글 ID 묶음 GROUP BY) 1
        assertThat(statistics.getPrepareStatementCount())
                .as("목록 조회 SQL 수")
                .isLessThanOrEqualTo(3);
        assertThat(statistics.getEntityFetchCount())
                .as("작성자 지연 로딩 횟수")
                .isZero();
    }

    // ===== 도우미 =====

    private String signupAndLogin(String prefix, String part) throws Exception {
        String email = prefix + "-" + tag + "@gmail.com";
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1234","name":"테스터",
                                 "nickname":"%s","generation":14,"part":"%s"}
                                """.formatted(email, prefix + "닉네임", part)))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password1234\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    private long createPost(String token, String title, String content) throws Exception {
        String body = mockMvc.perform(withToken(post("/api/study-posts"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.HashMap<>(java.util.Map.of(
                                "title", title, "content", content)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("postId").asLong();
    }

    private JsonNode list(String... params) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/study-posts");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data");
    }

    // "알고리즘"으로 검색한 결과 중 이 테스트가 만든 글만 남겨요.
    private JsonNode listByKeywordAnd(String keyword) throws Exception {
        JsonNode page = list("keyword", keyword, "size", "100");
        var filtered = objectMapper.createArrayNode();
        page.path("content").forEach(item -> {
            if (item.path("title").asText().startsWith(tag)) {
                filtered.add(item);
            }
        });
        return objectMapper.createObjectNode().set("content", filtered);
    }

    private List<Long> postIds(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.path("content").forEach(item -> ids.add(item.path("postId").asLong()));
        return ids;
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }
}
