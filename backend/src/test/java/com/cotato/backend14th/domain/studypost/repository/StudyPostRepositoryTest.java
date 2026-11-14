package com.cotato.backend14th.domain.studypost.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository.CommentCount;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

// 실제 로컬 MySQL에서 검색 쿼리의 필터 조합을 확인해요.
// @DataJpaTest는 테스트마다 트랜잭션을 롤백하므로 데이터가 남지 않아요.
// 다른 데이터와 섞이지 않게 매번 새 키워드(UUID)로 범위를 좁혀요.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StudyPostRepositoryTest {

    @Autowired
    private StudyPostRepository studyPostRepository;

    @Autowired
    private StudyPostCommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private String tag;
    private StudyPost beRecruiting;
    private StudyPost beClosed;
    private StudyPost feRecruiting;

    @BeforeEach
    void setUp() {
        tag = UUID.randomUUID().toString();
        User be = userRepository.save(user(Part.BE));
        User fe = userRepository.save(user(Part.FE));

        beRecruiting = studyPostRepository.save(post(be, "BE 모집 " + tag, "내용"));
        beClosed = studyPostRepository.save(post(be, "BE 마감", "본문에 " + tag));
        beClosed.update(null, null, StudyStatus.CLOSED);
        feRecruiting = studyPostRepository.save(post(fe, "FE 모집 " + tag, "내용"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("키워드는 제목과 내용 모두에서 찾고, 최신 글이 먼저 나온다")
    void search_keywordInTitleOrContent() {
        Page<StudyPost> result = search(tag, null, null);

        assertThat(ids(result)).containsExactly(feRecruiting.getId(), beClosed.getId(), beRecruiting.getId());
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("키워드, 상태, 파트를 함께 쓰면 모두 만족하는 글만 나온다")
    void search_allFiltersCombined() {
        assertThat(ids(search(tag, StudyStatus.RECRUITING, Part.BE))).containsExactly(beRecruiting.getId());
        assertThat(ids(search(tag, StudyStatus.CLOSED, Part.BE))).containsExactly(beClosed.getId());
        assertThat(ids(search(tag, StudyStatus.CLOSED, Part.FE))).isEmpty();
    }

    @Test
    @DisplayName("파트는 작성자의 파트로 거른다")
    void search_partFilter() {
        assertThat(ids(search(tag, null, Part.FE))).containsExactly(feRecruiting.getId());
        assertThat(ids(search(tag, null, Part.PM))).isEmpty();
    }

    @Test
    @DisplayName("상태만으로도 거를 수 있다")
    void search_statusFilter() {
        assertThat(ids(search(tag, StudyStatus.RECRUITING, null)))
                .containsExactly(feRecruiting.getId(), beRecruiting.getId());
    }

    @Test
    @DisplayName("페이지 크기만큼 나누고 전체 개수는 조건에 맞는 글 수다")
    void search_paging() {
        Page<StudyPost> secondPage = studyPostRepository.search(tag, null, null, PageRequest.of(1, 2));

        assertThat(ids(secondPage)).containsExactly(beRecruiting.getId());
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("댓글 수는 글별로 세고, 글을 지우기 전에 댓글을 한 번에 지울 수 있다")
    void commentCountAndBulkDelete() {
        User commenter = userRepository.save(user(Part.PM));
        StudyPost post = studyPostRepository.findById(beRecruiting.getId()).orElseThrow();
        commentRepository.save(comment(post, commenter));
        commentRepository.save(comment(post, commenter));
        // 실제 삭제 요청처럼 댓글을 불러오지 않은 새 영속성 컨텍스트에서 시작해요
        entityManager.flush();
        entityManager.clear();

        List<CommentCount> counts = commentRepository.countByPostIds(
                List.of(beRecruiting.getId(), feRecruiting.getId()));
        assertThat(counts).hasSize(1);
        assertThat(counts.get(0).getPostId()).isEqualTo(beRecruiting.getId());
        assertThat(counts.get(0).getCount()).isEqualTo(2);

        StudyPost target = studyPostRepository.findWithUserById(beRecruiting.getId()).orElseThrow();
        assertThat(commentRepository.deleteAllByPostId(target.getId())).isEqualTo(2);
        studyPostRepository.delete(target);
        entityManager.flush();
        assertThat(studyPostRepository.findById(beRecruiting.getId())).isEmpty();
    }

    private Page<StudyPost> search(String keyword, StudyStatus status, Part part) {
        return studyPostRepository.search(keyword, status, part, PageRequest.of(0, 10));
    }

    private List<Long> ids(Page<StudyPost> page) {
        return page.getContent().stream().map(StudyPost::getId).toList();
    }

    private User user(Part part) {
        return User.builder()
                .email("repo-test-" + UUID.randomUUID() + "@example.com")
                .password("encoded")
                .name("테스트")
                .nickname("테스터")
                .generation(14)
                .part(part)
                .build();
    }

    private StudyPost post(User author, String title, String content) {
        return StudyPost.builder().user(author).title(title).content(content).build();
    }

    private StudyPostComment comment(StudyPost post, User author) {
        return StudyPostComment.builder().studyPost(post).user(author).content("참여하고 싶어요").build();
    }
}
