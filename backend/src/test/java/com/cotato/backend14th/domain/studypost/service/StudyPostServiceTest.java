package com.cotato.backend14th.domain.studypost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository.PostCommentCount;
import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostListResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.service.UserService;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class StudyPostServiceTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long POST_ID = 10L;

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private StudyPostCommentRepository studyPostCommentRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private StudyPostService studyPostService;

    @Test
    @DisplayName("모집글을 작성하면 RECRUITING 상태로 저장된다")
    void create_success() {
        User author = user(AUTHOR_ID);
        given(userService.getUser(AUTHOR_ID)).willReturn(author);
        given(studyPostRepository.save(any(StudyPost.class))).willAnswer(invocation -> {
            StudyPost saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", POST_ID);
            assertThat(saved.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
            return saved;
        });

        Long postId = studyPostService.create(AUTHOR_ID, new StudyPostCreateRequest("제목", "내용")).getPostId();

        assertThat(postId).isEqualTo(POST_ID);
    }

    @Test
    @DisplayName("목록 조회는 댓글 수를 채워서 페이지 응답으로 돌려준다")
    void getList_withCommentCount() {
        StudyPost post = post(POST_ID, user(AUTHOR_ID));
        Pageable pageable = PageRequest.of(0, 10);
        given(studyPostRepository.search(eq("알고리즘"), eq(StudyStatus.RECRUITING), eq(Part.BE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post), pageable, 1));
        given(studyPostCommentRepository.countByStudyPostIds(List.of(POST_ID)))
                .willReturn(List.of(commentCount(POST_ID, 3L)));

        PageResponse<StudyPostListResponse> response =
                studyPostService.getList(" 알고리즘 ", StudyStatus.RECRUITING, Part.BE, 0, 10);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCommentCount()).isEqualTo(3L);
        assertThat(response.getContent().get(0).getAuthor().getPart()).isEqualTo(Part.BE);
    }

    @Test
    @DisplayName("키워드가 비어 있으면 조건 없이(null) 조회한다")
    void getList_blankKeyword() {
        given(studyPostRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PageResponse<StudyPostListResponse> response = studyPostService.getList("  ", null, null, 0, 10);

        assertThat(response.getContent()).isEmpty();
        verify(studyPostCommentRepository, never()).countByStudyPostIds(any());
    }

    @Test
    @DisplayName("상세 조회에서 작성자 본인이면 isAuthor가 true, 로그인하지 않았으면 false다")
    void getDetail_isAuthor() {
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post(POST_ID, user(AUTHOR_ID))));

        StudyPostDetailResponse mine = studyPostService.getDetail(POST_ID, AUTHOR_ID);
        StudyPostDetailResponse anonymous = studyPostService.getDetail(POST_ID, null);

        assertThat(mine.getIsAuthor()).isTrue();
        assertThat(anonymous.getIsAuthor()).isFalse();
    }

    @Test
    @DisplayName("없는 모집글을 조회하면 STUDY_POST_NOT_FOUND 예외가 난다")
    void getDetail_notFound() {
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyPostService.getDetail(POST_ID, null))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("수정은 보낸 필드만 바꾼다")
    void update_partial() {
        StudyPost post = post(POST_ID, user(AUTHOR_ID));
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

        studyPostService.update(POST_ID, AUTHOR_ID, new StudyPostUpdateRequest(null, null, StudyStatus.CLOSED));

        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).isEqualTo("내용");
        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
    }

    @Test
    @DisplayName("작성자가 아니면 수정할 수 없다")
    void update_notAuthor() {
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post(POST_ID, user(AUTHOR_ID))));

        assertThatThrownBy(() -> studyPostService.update(
                POST_ID, OTHER_ID, new StudyPostUpdateRequest("바꾼 제목", null, null)))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);
    }

    @Test
    @DisplayName("삭제하면 댓글을 먼저 지우고 글을 지운다")
    void delete_withComments() {
        StudyPost post = post(POST_ID, user(AUTHOR_ID));
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

        studyPostService.delete(POST_ID, AUTHOR_ID);

        InOrder inOrder = inOrder(studyPostCommentRepository, studyPostRepository);
        inOrder.verify(studyPostCommentRepository).deleteAllByStudyPostId(POST_ID);
        inOrder.verify(studyPostRepository).delete(post);
    }

    @Test
    @DisplayName("작성자가 아니면 삭제할 수 없다")
    void delete_notAuthor() {
        StudyPost post = post(POST_ID, user(AUTHOR_ID));
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> studyPostService.delete(POST_ID, OTHER_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);
        verify(studyPostRepository, never()).delete(any(StudyPost.class));
    }

    private User user(Long id) {
        User user = User.builder()
                .email("user" + id + "@example.com")
                .password("encoded-password")
                .name("홍길동")
                .nickname("길동이")
                .generation(14)
                .part(Part.BE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private StudyPost post(Long id, User author) {
        StudyPost post = new StudyPost(author, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", id);
        return post;
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
}
