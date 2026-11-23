package com.cotato.backend14th.domain.studypost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository.CommentCount;
import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostSummaryResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
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
    private StudyPostCommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyPostService studyPostService;

    @Test
    @DisplayName("모집글을 작성하면 작성자와 함께 RECRUITING 상태로 저장하고 postId를 돌려준다")
    void create_success() {
        User author = user(AUTHOR_ID, Part.BE);
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(author));
        given(studyPostRepository.save(any(StudyPost.class))).willAnswer(invocation -> {
            StudyPost saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", POST_ID);
            return saved;
        });

        Long postId = studyPostService.create(AUTHOR_ID, new StudyPostCreateRequest("알고리즘 스터디", "주 2회")).postId();

        assertThat(postId).isEqualTo(POST_ID);
        ArgumentCaptor<StudyPost> captor = ArgumentCaptor.forClass(StudyPost.class);
        verify(studyPostRepository).save(captor.capture());
        StudyPost saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(author);
        assertThat(saved.getTitle()).isEqualTo("알고리즘 스터디");
        assertThat(saved.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
    }

    @Test
    @DisplayName("목록 조회는 키워드, 상태, 파트를 함께 넘기고 공백 키워드는 조건에서 뺀다")
    void search_passesFilters() {
        given(studyPostRepository.search(eq(null), eq(StudyStatus.CLOSED), eq(Part.FE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PageResponse<StudyPostSummaryResponse> result =
                studyPostService.search("   ", StudyStatus.CLOSED, Part.FE, 0, 10);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        // 글이 없으면 댓글 수 쿼리를 보내지 않는다
        verify(commentRepository, never()).countByPostIds(any());
    }

    @Test
    @DisplayName("목록의 댓글 수는 한 번의 쿼리로 세고, 댓글이 없는 글은 0으로 채운다")
    void search_commentCounts() {
        StudyPost withComments = post(POST_ID, user(AUTHOR_ID, Part.BE));
        StudyPost withoutComments = post(11L, user(OTHER_ID, Part.FE));
        given(studyPostRepository.search(eq("알고리즘"), eq(null), eq(null), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(withComments, withoutComments), PageRequest.of(0, 10), 2));
        given(commentRepository.countByPostIds(List.of(POST_ID, 11L)))
                .willReturn(List.of(commentCount(POST_ID, 3)));

        PageResponse<StudyPostSummaryResponse> result =
                studyPostService.search(" 알고리즘 ", null, null, 0, 10);

        assertThat(result.content()).extracting(StudyPostSummaryResponse::commentCount).containsExactly(3L, 0L);
        assertThat(result.content().get(0).author().part()).isEqualTo(Part.BE);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("상세 조회는 로그인한 작성자에게만 isAuthor를 true로 준다")
    void getDetail_isAuthor() {
        StudyPost post = post(POST_ID, user(AUTHOR_ID, Part.BE));
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

        StudyPostDetailResponse asAuthor = studyPostService.getDetail(POST_ID, AUTHOR_ID);
        StudyPostDetailResponse asOther = studyPostService.getDetail(POST_ID, OTHER_ID);
        StudyPostDetailResponse asGuest = studyPostService.getDetail(POST_ID, null);

        assertThat(asAuthor.isAuthor()).isTrue();
        assertThat(asOther.isAuthor()).isFalse();
        assertThat(asGuest.isAuthor()).isFalse();
    }

    @Test
    @DisplayName("없는 모집글을 조회하면 STUDY_POST_NOT_FOUND를 던진다")
    void getDetail_notFound() {
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyPostService.getDetail(POST_ID, AUTHOR_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자가 수정하면 보낸 필드만 바뀐다")
    void update_onlySentFields() {
        StudyPost post = post(POST_ID, user(AUTHOR_ID, Part.BE));
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

        studyPostService.update(POST_ID, AUTHOR_ID, new StudyPostUpdateRequest(null, null, StudyStatus.CLOSED));

        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).isEqualTo("내용");
    }

    @Test
    @DisplayName("작성자가 아니면 수정할 때 NOT_POST_AUTHOR를 던지고 글은 그대로다")
    void update_notAuthor() {
        StudyPost post = post(POST_ID, user(AUTHOR_ID, Part.BE));
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> studyPostService.update(POST_ID, OTHER_ID,
                new StudyPostUpdateRequest("바꾼 제목", null, null)))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);
        assertThat(post.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("없는 모집글을 수정하면 STUDY_POST_NOT_FOUND를 던진다")
    void update_notFound() {
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyPostService.update(POST_ID, AUTHOR_ID,
                new StudyPostUpdateRequest("바꾼 제목", null, null)))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자가 삭제하면 댓글을 먼저 지우고 글을 지운다")
    void delete_success() {
        StudyPost post = post(POST_ID, user(AUTHOR_ID, Part.BE));
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

        studyPostService.delete(POST_ID, AUTHOR_ID);

        verify(commentRepository).deleteAllByPostId(POST_ID);
        verify(studyPostRepository).delete(post);
    }

    @Test
    @DisplayName("작성자가 아니면 삭제할 때 NOT_POST_AUTHOR를 던지고 아무것도 지우지 않는다")
    void delete_notAuthor() {
        StudyPost post = post(POST_ID, user(AUTHOR_ID, Part.BE));
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> studyPostService.delete(POST_ID, OTHER_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);
        verify(commentRepository, never()).deleteAllByPostId(anyLong());
        verify(studyPostRepository, never()).delete(any());
    }

    @Test
    @DisplayName("없는 모집글을 삭제하면 STUDY_POST_NOT_FOUND를 던진다")
    void delete_notFound() {
        given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyPostService.delete(POST_ID, AUTHOR_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    private User user(Long id, Part part) {
        User user = User.builder()
                .email("user" + id + "@gmail.com")
                .password("encoded")
                .name("이름")
                .nickname("닉네임" + id)
                .generation(14)
                .part(part)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private StudyPost post(Long id, User author) {
        StudyPost post = StudyPost.builder().user(author).title("제목").content("내용").build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private CommentCount commentCount(Long postId, long count) {
        return new CommentCount() {
            @Override
            public Long getPostId() {
                return postId;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }
}
