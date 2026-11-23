package com.cotato.backend14th.domain.comment;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 MySQL로 댓글 CRUD, 작성자 권한, 모집 마감 차단, 모집글 삭제 시 댓글 삭제,
// 목록 정렬·페이징, 모집글 목록의 commentCount, N+1 여부를 확인해요.
// 테스트가 끝나면 @Transactional로 모두 롤백해요.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Transactional
class CommentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private StudyPostCommentRepository commentRepository;

    private String tag;
    private String hostToken;      // 모집글 작성자 (BE)
    private String applicantToken; // 신청자 (FE)
    private long postId;

    @BeforeEach
    void setUp() throws Exception {
        tag = "cm" + UUID.randomUUID().toString().substring(0, 8);
        hostToken = signupAndLogin("host", "BE");
        applicantToken = signupAndLogin("apply", "FE");
        postId = createPost(hostToken, tag + " 알고리즘 스터디");
    }

    @Test
    @DisplayName("댓글 작성 → 목록 → 수정 → 삭제 흐름과 작성자 권한을 확인해요")
    void crudAndAuthorization() throws Exception {
        long first = createComment(applicantToken, postId, "참여하고 싶어요!");
        long second = createComment(hostToken, postId, "환영해요.");

        // 목록: 오래된 순, 작성자 정보, 현재 사용자 기준 isAuthor
        JsonNode asApplicant = listComments(postId, applicantToken);
        assertThat(commentIds(asApplicant)).containsExactly(first, second);
        assertThat(asApplicant.path("content").get(0).path("content").asText()).isEqualTo("참여하고 싶어요!");
        assertThat(asApplicant.path("content").get(0).path("author").path("part").asText()).isEqualTo("FE");
        assertThat(asApplicant.path("content").get(0).path("isAuthor").asBoolean()).isTrue();
        assertThat(asApplicant.path("content").get(1).path("isAuthor").asBoolean()).isFalse();
        assertThat(asApplicant.path("content").get(0).has("createdAt")).isTrue();
        assertThat(asApplicant.path("size").asInt()).isEqualTo(20);
        assertThat(asApplicant.path("totalElements").asLong()).isEqualTo(2);

        JsonNode asGuest = listComments(postId, null);
        assertThat(asGuest.path("content").get(0).path("isAuthor").asBoolean()).isFalse();
        assertThat(asGuest.path("content").get(1).path("isAuthor").asBoolean()).isFalse();

        // 다른 사용자(모집글 작성자 포함)는 남의 댓글을 수정·삭제할 수 없어요
        mockMvc.perform(withToken(patch("/api/comments/" + first), hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"가로채기\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT-002"));
        mockMvc.perform(withToken(delete("/api/comments/" + first), hostToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT-002"));

        // 작성자는 수정할 수 있어요
        mockMvc.perform(withToken(patch("/api/comments/" + first), applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 신청 메시지\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value(first));
        assertThat(listComments(postId, null).path("content").get(0).path("content").asText())
                .isEqualTo("수정된 신청 메시지");

        // 작성자는 삭제할 수 있고, 삭제 후에는 404예요
        mockMvc.perform(withToken(delete("/api/comments/" + first), applicantToken))
                .andExpect(status().isNoContent());
        assertThat(commentIds(listComments(postId, null))).containsExactly(second);
        mockMvc.perform(withToken(patch("/api/comments/" + first), applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT-001"));
        mockMvc.perform(withToken(delete("/api/comments/" + first), applicantToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("모집이 마감된 글에는 댓글을 작성할 수 없고(409), 기존 댓글은 그대로 조회돼요")
    void closedPostBlocksNewComments() throws Exception {
        long existing = createComment(applicantToken, postId, "마감 전 신청");

        mockMvc.perform(withToken(patch("/api/study-posts/" + postId), hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studyStatus\":\"CLOSED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(withToken(post("/api/study-posts/" + postId + "/comments"), applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"마감 후 신청\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY-003"));

        assertThat(commentIds(listComments(postId, null))).containsExactly(existing);

        // 다시 모집 중으로 바꾸면 작성할 수 있어요
        mockMvc.perform(withToken(patch("/api/study-posts/" + postId), hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studyStatus\":\"RECRUITING\"}"))
                .andExpect(status().isOk());
        createComment(applicantToken, postId, "재모집 신청");
    }

    @Test
    @DisplayName("없는 글에 대한 댓글 작성·조회는 404, 토큰 없이 작성하면 401이에요")
    void errors() throws Exception {
        mockMvc.perform(withToken(post("/api/study-posts/" + Long.MAX_VALUE + "/comments"), applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내용\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY-001"));
        mockMvc.perform(get("/api/study-posts/" + Long.MAX_VALUE + "/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY-001"));
        mockMvc.perform(post("/api/study-posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내용\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON-004"));
    }

    @Test
    @DisplayName("모집글을 삭제하면 그 글의 댓글도 함께 삭제돼요")
    void deletingPostDeletesComments() throws Exception {
        createComment(applicantToken, postId, "신청 1");
        createComment(hostToken, postId, "신청 2");
        long otherPostId = createPost(hostToken, tag + " 다른 스터디");
        long otherComment = createComment(applicantToken, otherPostId, "다른 글 신청");
        assertThat(commentRepository.countByStudyPostId(postId)).isEqualTo(2);

        mockMvc.perform(withToken(delete("/api/study-posts/" + postId), hostToken))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        assertThat(commentRepository.countByStudyPostId(postId)).isZero();
        mockMvc.perform(get("/api/study-posts/" + postId + "/comments"))
                .andExpect(status().isNotFound());

        // 다른 글의 댓글은 그대로예요
        assertThat(commentIds(listComments(otherPostId, null))).containsExactly(otherComment);
    }

    @Test
    @DisplayName("댓글 목록 페이징과 모집글 목록의 commentCount가 정확해요")
    void pagingAndCommentCount() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ids.add(createComment(i % 2 == 0 ? applicantToken : hostToken, postId, "댓글 " + i));
        }
        long emptyPostId = createPost(hostToken, tag + " 댓글 없는 글");

        JsonNode page0 = listComments(postId, null, "page", "0", "size", "2");
        JsonNode page2 = listComments(postId, null, "page", "2", "size", "2");
        assertThat(commentIds(page0)).containsExactly(ids.get(0), ids.get(1));
        assertThat(commentIds(page2)).containsExactly(ids.get(4));
        assertThat(page2.path("page").asInt()).isEqualTo(2);
        assertThat(page2.path("totalElements").asLong()).isEqualTo(5);
        assertThat(page2.path("totalPages").asInt()).isEqualTo(3);

        // 모집글 목록의 commentCount
        String body = mockMvc.perform(get("/api/study-posts").param("keyword", tag))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<Long, Long> counts = new java.util.HashMap<>();
        objectMapper.readTree(body).path("data").path("content").forEach(item ->
                counts.put(item.path("postId").asLong(), item.path("commentCount").asLong()));
        assertThat(counts).containsEntry(postId, 5L).containsEntry(emptyPostId, 0L);
    }

    @Test
    @DisplayName("작성자가 여러 명이어도 댓글 목록 조회에 N+1이 없어요")
    void list_noNPlusOne() throws Exception {
        for (int i = 0; i < 3; i++) {
            createComment(applicantToken, postId, "신청자 댓글 " + i);
            createComment(hostToken, postId, "작성자 댓글 " + i);
        }
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        JsonNode page = listComments(postId, null, "size", "4");

        assertThat(page.path("content")).hasSize(4);
        assertThat(page.path("totalElements").asLong()).isEqualTo(6);
        // 글 존재 확인 1 + 목록 1 + 전체 개수 1
        assertThat(statistics.getPrepareStatementCount()).as("댓글 목록 SQL 수").isLessThanOrEqualTo(3);
        assertThat(statistics.getEntityFetchCount()).as("작성자 지연 로딩 횟수").isZero();
    }

    // ===== 도우미 =====

    private String signupAndLogin(String prefix, String part) throws Exception {
        String email = prefix + "-" + tag + "@gmail.com";
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1234","name":"테스터",
                                 "nickname":"%s","generation":14,"part":"%s"}
                                """.formatted(email, prefix, part)))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password1234\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    private long createPost(String token, String title) throws Exception {
        String body = mockMvc.perform(withToken(post("/api/study-posts"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", title, "content", "내용"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("postId").asLong();
    }

    private long createComment(String token, long targetPostId, String content) throws Exception {
        String body = mockMvc.perform(withToken(post("/api/study-posts/" + targetPostId + "/comments"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("commentId").asLong();
    }

    private JsonNode listComments(long targetPostId, String token, String... params) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/study-posts/" + targetPostId + "/comments");
        if (token != null) {
            withToken(request, token);
        }
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data");
    }

    private List<Long> commentIds(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.path("content").forEach(item -> ids.add(item.path("commentId").asLong()));
        return ids;
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }
}
