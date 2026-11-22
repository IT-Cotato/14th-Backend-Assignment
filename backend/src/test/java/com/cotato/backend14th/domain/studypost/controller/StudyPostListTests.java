package com.cotato.backend14th.domain.studypost.controller;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(JwtTestConfiguration.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:study-post-list")
@AutoConfigureMockMvc
@Transactional
class StudyPostListTests {

    private static final String PATH = "/api/study-posts";
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 11, 8, 12, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private StudyPostRepository posts;
    @Autowired private EntityManager entityManager;

    private StudyPost newest;
    private StudyPost closed;
    private StudyPost frontend;
    private StudyPost oldest;

    @BeforeEach
    void setUpPosts() {
        User backendAuthor = users.save(new User("be@example.com", "test-hash", "홍길동", "백엔드", 14, Part.BE));
        User frontendAuthor = users.save(new User("fe@example.com", "test-hash", "김철수", "프론트", 13, Part.FE));
        newest = savePost("Spring 100%_ 실습", "프로젝트 모집", backendAuthor, StudyStatus.RECRUITING, 3);
        closed = savePost("알고리즘", "SPRING 개발자 환영", backendAuthor, StudyStatus.CLOSED, 2);
        frontend = savePost("Spring UI", "프론트엔드 모집", frontendAuthor, StudyStatus.RECRUITING, 2);
        oldest = savePost("데이터베이스", "SQL 실습", backendAuthor, StudyStatus.RECRUITING, 0);
        entityManager.persist(new StudyPostComment("첫 댓글", newest, frontendAuthor));
        entityManager.persist(new StudyPostComment("두 번째 댓글", newest, backendAuthor));
        entityManager.persist(new StudyPostComment("다른 글 댓글", oldest, frontendAuthor));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void listsPostsWithoutLoginInNewestOrderWithAuthorsAndCounts() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[*].postId",
                        contains(id(newest), id(frontend), id(closed), id(oldest))))
                .andExpect(jsonPath("$.data.content[0].title").value("Spring 100%_ 실습"))
                .andExpect(jsonPath("$.data.content[0].studyStatus").value("RECRUITING"))
                .andExpect(jsonPath("$.data.content[0].author.nickname").value("백엔드"))
                .andExpect(jsonPath("$.data.content[0].author.generation").value(14))
                .andExpect(jsonPath("$.data.content[0].author.part").value("BE"))
                .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-11-08T12:03:00"))
                .andExpect(jsonPath("$.data.content[*].commentCount", contains(2, 0, 0, 1)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void searchesTitleOrContentIgnoringCaseAndTreatsWildcardsLiterally() throws Exception {
        expectPosts(mockMvc.perform(get(PATH).param("keyword", "spring")), newest, frontend, closed);
        expectPosts(mockMvc.perform(get(PATH).param("keyword", "%_")), newest);
    }

    @Test
    void filtersByStatusAndAuthorPartSeparately() throws Exception {
        expectPosts(mockMvc.perform(get(PATH).param("status", "CLOSED")), closed);
        expectPosts(mockMvc.perform(get(PATH).param("part", "FE")), frontend);
    }

    @Test
    void combinesKeywordStatusAndPartWithAnd() throws Exception {
        expectPosts(mockMvc.perform(get(PATH).param("keyword", "spring")
                .param("status", "RECRUITING").param("part", "BE")), newest);
    }

    @Test
    void paginatesFilteredResultsAndReturnsMatchingTotals() throws Exception {
        ResultActions result = mockMvc.perform(get(PATH).param("status", "RECRUITING")
                .param("page", "1").param("size", "1"));
        expectPosts(result, frontend);
        result.andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(3));
    }

    @Test
    void returnsEmptyArrayForNoMatchesAndOutOfRangePages() throws Exception {
        mockMvc.perform(get(PATH).param("keyword", "없는 키워드"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0));
        mockMvc.perform(get(PATH).param("page", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(4));
    }

    @ParameterizedTest
    @CsvSource({"status, OPEN", "part, XYZ", "page, -1", "page, 1.5", "page, 2147483647",
            "size, 0", "size, -1", "size, x", "size, ''", "status, ''"})
    void rejectsInvalidParameters(String key, String value) throws Exception {
        mockMvc.perform(get(PATH).param(key, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-003"));
    }

    private StudyPost savePost(String title, String content, User author, StudyStatus status, int minutes) {
        StudyPost post = posts.save(new StudyPost(title, content, author));
        entityManager.createQuery("""
                        update StudyPost sp set sp.createdAt = :createdAt, sp.studyStatus = :status where sp.id = :id
                        """)
                .setParameter("createdAt", BASE_TIME.plusMinutes(minutes))
                .setParameter("status", status)
                .setParameter("id", post.getId())
                .executeUpdate();
        return post;
    }

    private void expectPosts(ResultActions result, StudyPost... expected) throws Exception {
        Integer[] ids = java.util.Arrays.stream(expected).map(this::id).toArray(Integer[]::new);
        result.andExpect(status().isOk()).andExpect(jsonPath("$.data.content[*].postId", contains(ids)));
    }

    private int id(StudyPost post) {
        return post.getId().intValue();
    }
}
