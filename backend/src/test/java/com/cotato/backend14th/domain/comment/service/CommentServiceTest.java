package com.cotato.backend14th.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
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
class CommentServiceTest {

    private static final Long POST_AUTHOR_ID = 1L;
    private static final Long COMMENT_AUTHOR_ID = 2L;
    private static final Long OTHER_ID = 3L;
    private static final Long POST_ID = 10L;
    private static final Long COMMENT_ID = 5L;

    @Mock
    private StudyPostCommentRepository studyPostCommentRepository;

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("모집 중인 글에 댓글을 작성할 수 있다")
    void create_success() {
        StudyPost post = post();
        User commenter = user(COMMENT_AUTHOR_ID);
        given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(userService.getUser(COMMENT_AUTHOR_ID)).willReturn(commenter);
        given(studyPostCommentRepository.save(any(StudyPostComment.class))).willAnswer(invocation -> {
            StudyPostComment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", COMMENT_ID);
            return saved;
        });

        Long commentId = commentService.create(POST_ID, COMMENT_AUTHOR_ID, new CommentRequest("신청합니다")).getCommentId();

        assertThat(commentId).isEqualTo(COMMENT_ID);
    }

    @Test
    @DisplayName("마감된 글에 댓글을 쓰면 STUDY_POST_CLOSED 예외가 난다")
    void create_closedPost() {
        StudyPost post = post();
        post.update(null, null, StudyStatus.CLOSED);
        given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.create(POST_ID, COMMENT_AUTHOR_ID, new CommentRequest("신청합니다")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_CLOSED);
        verify(studyPostCommentRepository, never()).save(any(StudyPostComment.class));
    }

    @Test
    @DisplayName("없는 글에 댓글을 쓰면 STUDY_POST_NOT_FOUND 예외가 난다")
    void create_postNotFound() {
        given(studyPostRepository.findById(POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(POST_ID, COMMENT_AUTHOR_ID, new CommentRequest("신청합니다")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 목록은 내가 쓴 댓글에만 isAuthor가 true다")
    void getList_isAuthor() {
        StudyPostComment comment = comment(post(), user(COMMENT_AUTHOR_ID));
        given(studyPostRepository.existsById(POST_ID)).willReturn(true);
        given(studyPostCommentRepository.findPageByStudyPostId(eq(POST_ID), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment), PageRequest.of(0, 20), 1));

        PageResponse<CommentResponse> mine = commentService.getList(POST_ID, COMMENT_AUTHOR_ID, 0, 20);
        PageResponse<CommentResponse> others = commentService.getList(POST_ID, OTHER_ID, 0, 20);

        assertThat(mine.getContent().get(0).getIsAuthor()).isTrue();
        assertThat(others.getContent().get(0).getIsAuthor()).isFalse();
    }

    @Test
    @DisplayName("없는 글의 댓글 목록을 조회하면 STUDY_POST_NOT_FOUND 예외가 난다")
    void getList_postNotFound() {
        given(studyPostRepository.existsById(POST_ID)).willReturn(false);

        assertThatThrownBy(() -> commentService.getList(POST_ID, null, 0, 20))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자는 댓글 내용을 수정할 수 있다")
    void update_success() {
        StudyPostComment comment = comment(post(), user(COMMENT_AUTHOR_ID));
        given(studyPostCommentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

        commentService.update(COMMENT_ID, COMMENT_AUTHOR_ID, new CommentRequest("수정한 신청 메시지"));

        assertThat(comment.getContent()).isEqualTo("수정한 신청 메시지");
    }

    @Test
    @DisplayName("작성자가 아니면 댓글을 수정할 수 없다")
    void update_notAuthor() {
        StudyPostComment comment = comment(post(), user(COMMENT_AUTHOR_ID));
        given(studyPostCommentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(COMMENT_ID, OTHER_ID, new CommentRequest("수정")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
        assertThat(comment.getContent()).isEqualTo("신청합니다");
    }

    @Test
    @DisplayName("없는 댓글을 삭제하면 COMMENT_NOT_FOUND 예외가 난다")
    void delete_notFound() {
        given(studyPostCommentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(COMMENT_ID, COMMENT_AUTHOR_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자가 아니면 댓글을 삭제할 수 없다")
    void delete_notAuthor() {
        StudyPostComment comment = comment(post(), user(COMMENT_AUTHOR_ID));
        given(studyPostCommentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(COMMENT_ID, OTHER_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
        verify(studyPostCommentRepository, never()).delete(any(StudyPostComment.class));
    }

    private User user(Long id) {
        User user = User.builder()
                .email("user" + id + "@example.com")
                .password("encoded-password")
                .name("홍길동")
                .nickname("길동이")
                .generation(14)
                .part(Part.FE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private StudyPost post() {
        StudyPost post = new StudyPost(user(POST_AUTHOR_ID), "제목", "내용");
        ReflectionTestUtils.setField(post, "id", POST_ID);
        return post;
    }

    private StudyPostComment comment(StudyPost post, User author) {
        StudyPostComment comment = new StudyPostComment(post, author, "신청합니다");
        ReflectionTestUtils.setField(comment, "id", COMMENT_ID);
        return comment;
    }
}
