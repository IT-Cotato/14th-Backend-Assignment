package com.cotato.backend14th.domain.studypost.service;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository.PostCommentCount;
import org.mockito.InOrder;

import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class StudyPostServiceTest {

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyPostCommentRepository commentRepository;

    @InjectMocks
    private StudyPostService studyPostService;

    private User author;
    private StudyPost post;

    @BeforeEach
    void setUp() {
        author = new User("author@gmail.com", "encoded", "홍길동", "길동이", 14, Part.BE);
        ReflectionTestUtils.setField(author, "id", 1L);
        post = new StudyPost(author, "알고리즘 스터디", "주 2회 백준");
        ReflectionTestUtils.setField(post, "id", 10L);
    }

    @Test
    @DisplayName("작성하면 로그인한 사용자가 작성자이고 상태는 RECRUITING이에요")
    void create() {
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(studyPostRepository.save(any(StudyPost.class))).willAnswer(invocation -> {
            StudyPost saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        Long postId = studyPostService.create(1L, new StudyPostCreateRequest("제목", "내용")).postId();

        ArgumentCaptor<StudyPost> captor = ArgumentCaptor.forClass(StudyPost.class);
        then(studyPostRepository).should().save(captor.capture());
        assertThat(postId).isEqualTo(10L);
        assertThat(captor.getValue().getUser()).isSameAs(author);
        assertThat(captor.getValue().getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
    }

    @Test
    @DisplayName("토큰의 회원이 없으면 작성할 수 없어요 (401)")
    void create_userNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertErrorCode(
                () -> studyPostService.create(99L, new StudyPostCreateRequest("제목", "내용")),
                ErrorCode.UNAUTHORIZED
        );
        then(studyPostRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("목록 검색 시 공백 키워드는 조건에서 빼고, 앞뒤 공백은 잘라요")
    void getStudyPosts_normalizesKeyword() {
        given(studyPostRepository.search(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1));

        var blank = studyPostService.getStudyPosts("   ", null, null, 0, 10);
        studyPostService.getStudyPosts("  알고리즘 ", StudyStatus.RECRUITING, Part.BE, 0, 10);

        then(studyPostRepository).should().search(eq(null), eq(null), eq(null), eq(PageRequest.of(0, 10)));
        then(studyPostRepository).should().search(
                eq("알고리즘"), eq(StudyStatus.RECRUITING), eq(Part.BE), eq(PageRequest.of(0, 10)));
        assertThat(blank.totalElements()).isEqualTo(1);
        assertThat(blank.content().get(0).author().nickname()).isEqualTo("길동이");
        assertThat(blank.content().get(0).commentCount()).isZero();
    }

    @Test
    @DisplayName("목록의 댓글 수는 글 ID 목록으로 한 번에 조회하고, 댓글이 없는 글은 0이에요")
    void getStudyPosts_commentCount() {
        StudyPost other = new StudyPost(author, "다른 글", "내용");
        ReflectionTestUtils.setField(other, "id", 11L);
        given(studyPostRepository.search(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(post, other), PageRequest.of(0, 10), 2));
        given(commentRepository.countByStudyPostIds(List.of(10L, 11L)))
                .willReturn(List.of(commentCount(10L, 3L)));

        var result = studyPostService.getStudyPosts(null, null, null, 0, 10);

        assertThat(result.content().get(0).commentCount()).isEqualTo(3);
        assertThat(result.content().get(1).commentCount()).isZero();
        then(commentRepository).should().countByStudyPostIds(List.of(10L, 11L));
    }

    @Test
    @DisplayName("목록이 비어 있으면 댓글 수 쿼리를 실행하지 않아요")
    void getStudyPosts_empty() {
        given(studyPostRepository.search(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        assertThat(studyPostService.getStudyPosts(null, null, null, 0, 10).content()).isEmpty();
        then(commentRepository).should(never()).countByStudyPostIds(any());
    }

    @Test
    @DisplayName("상세 조회의 isAuthor는 작성자 본인일 때만 true예요")
    void getStudyPost_isAuthor() {
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post));

        StudyPostDetailResponse asAuthor = studyPostService.getStudyPost(10L, 1L);
        StudyPostDetailResponse asOther = studyPostService.getStudyPost(10L, 2L);
        StudyPostDetailResponse asGuest = studyPostService.getStudyPost(10L, null);

        assertThat(asAuthor.isAuthor()).isTrue();
        assertThat(asOther.isAuthor()).isFalse();
        assertThat(asGuest.isAuthor()).isFalse();
    }

    @Test
    @DisplayName("없는 글을 조회하면 404예요")
    void getStudyPost_notFound() {
        given(studyPostRepository.findWithUserById(999L)).willReturn(Optional.empty());

        assertErrorCode(() -> studyPostService.getStudyPost(999L, null), ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("PATCH는 전달한 필드만 바꿔요")
    void update_partial() {
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        studyPostService.update(10L, 1L, new StudyPostUpdateRequest(null, null, StudyStatus.CLOSED));

        assertThat(post.getTitle()).isEqualTo("알고리즘 스터디");
        assertThat(post.getContent()).isEqualTo("주 2회 백준");
        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);

        studyPostService.update(10L, 1L, new StudyPostUpdateRequest("새 제목", null, null));

        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
    }

    @Test
    @DisplayName("작성자가 아니면 수정할 수 없어요 (403)")
    void update_notAuthor() {
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        assertErrorCode(
                () -> studyPostService.update(10L, 2L, new StudyPostUpdateRequest("x", null, null)),
                ErrorCode.NOT_POST_AUTHOR
        );
        assertThat(post.getTitle()).isEqualTo("알고리즘 스터디");
    }

    @Test
    @DisplayName("없는 글은 수정·삭제할 수 없어요 (404)")
    void updateAndDelete_notFound() {
        given(studyPostRepository.findById(999L)).willReturn(Optional.empty());

        assertErrorCode(
                () -> studyPostService.update(999L, 1L, new StudyPostUpdateRequest("x", null, null)),
                ErrorCode.STUDY_POST_NOT_FOUND
        );
        assertErrorCode(() -> studyPostService.delete(999L, 1L), ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자는 삭제할 수 있고(댓글 먼저 삭제), 다른 사람은 403이에요")
    void delete() {
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        assertErrorCode(() -> studyPostService.delete(10L, 2L), ErrorCode.NOT_POST_AUTHOR);
        then(studyPostRepository).should(never()).delete(any());
        then(commentRepository).should(never()).deleteAllByStudyPostId(any());

        studyPostService.delete(10L, 1L);

        InOrder inOrder = inOrder(commentRepository, studyPostRepository);
        inOrder.verify(commentRepository).deleteAllByStudyPostId(10L);
        inOrder.verify(studyPostRepository).delete(post);
    }

    private PostCommentCount commentCount(Long postId, Long count) {
        return new PostCommentCount() {
            @Override
            public Long getPostId() {
                return postId;
            }

            @Override
            public Long getCommentCount() {
                return count;
            }
        };
    }

    private void assertErrorCode(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
