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
import org.mockito.ArgumentCaptor;
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
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class StudyPostCommentServiceTest {

    private static final Long POST_AUTHOR_ID = 1L;
    private static final Long COMMENTER_ID = 2L;
    private static final Long OTHER_ID = 3L;
    private static final Long POST_ID = 10L;
    private static final Long COMMENT_ID = 100L;

    @Mock
    private StudyPostCommentRepository commentRepository;

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyPostCommentService commentService;

    @Test
    @DisplayName("모집 중인 글에 댓글을 쓰면 글과 작성자를 연결해 저장하고 commentId를 돌려준다")
    void create_success() {
        StudyPost post = post(StudyStatus.RECRUITING);
        User commenter = user(COMMENTER_ID);
        given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(userRepository.findById(COMMENTER_ID)).willReturn(Optional.of(commenter));
        given(commentRepository.save(any(StudyPostComment.class))).willAnswer(invocation -> {
            StudyPostComment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", COMMENT_ID);
            return saved;
        });

        Long commentId = commentService.create(POST_ID, COMMENTER_ID, new CommentRequest("참여하고 싶어요")).commentId();

        assertThat(commentId).isEqualTo(COMMENT_ID);
        ArgumentCaptor<StudyPostComment> captor = ArgumentCaptor.forClass(StudyPostComment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getStudyPost()).isSameAs(post);
        assertThat(captor.getValue().getUser()).isSameAs(commenter);
        assertThat(captor.getValue().getContent()).isEqualTo("참여하고 싶어요");
    }

    @Test
    @DisplayName("모집이 마감된 글에 댓글을 쓰면 STUDY_POST_CLOSED를 던지고 저장하지 않는다")
    void create_closedPost() {
        given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post(StudyStatus.CLOSED)));

        assertThatThrownBy(() -> commentService.create(POST_ID, COMMENTER_ID, new CommentRequest("참여하고 싶어요")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_CLOSED);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 글에 댓글을 쓰면 STUDY_POST_NOT_FOUND를 던진다")
    void create_postNotFound() {
        given(studyPostRepository.findById(POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(POST_ID, COMMENTER_ID, new CommentRequest("참여하고 싶어요")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 목록은 로그인한 작성자의 댓글에만 isAuthor를 true로 준다")
    void getComments_isAuthor() {
        StudyPost post = post(StudyStatus.RECRUITING);
        StudyPostComment mine = comment(COMMENT_ID, post, user(COMMENTER_ID));
        StudyPostComment others = comment(101L, post, user(OTHER_ID));
        given(studyPostRepository.existsById(POST_ID)).willReturn(true);
        given(commentRepository.findPageByPostId(eq(POST_ID), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(mine, others), PageRequest.of(0, 20), 2));

        PageResponse<CommentResponse> result = commentService.getComments(POST_ID, COMMENTER_ID, 0, 20);

        assertThat(result.content()).extracting(CommentResponse::isAuthor).containsExactly(true, false);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("없는 글의 댓글 목록을 보면 STUDY_POST_NOT_FOUND를 던진다")
    void getComments_postNotFound() {
        given(studyPostRepository.existsById(POST_ID)).willReturn(false);

        assertThatThrownBy(() -> commentService.getComments(POST_ID, null, 0, 20))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자가 댓글을 수정하면 내용이 바뀐다")
    void update_success() {
        StudyPostComment comment = comment(COMMENT_ID, post(StudyStatus.RECRUITING), user(COMMENTER_ID));
        given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

        commentService.update(COMMENT_ID, COMMENTER_ID, new CommentRequest("수정한 신청 메시지"));

        assertThat(comment.getContent()).isEqualTo("수정한 신청 메시지");
    }

    @Test
    @DisplayName("작성자가 아니면 댓글을 수정할 때 NOT_COMMENT_AUTHOR를 던지고 내용은 그대로다")
    void update_notAuthor() {
        StudyPostComment comment = comment(COMMENT_ID, post(StudyStatus.RECRUITING), user(COMMENTER_ID));
        given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(COMMENT_ID, OTHER_ID, new CommentRequest("남의 댓글 수정")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
        assertThat(comment.getContent()).isEqualTo("참여하고 싶어요");
    }

    @Test
    @DisplayName("없는 댓글을 수정하면 COMMENT_NOT_FOUND를 던진다")
    void update_notFound() {
        given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(COMMENT_ID, COMMENTER_ID, new CommentRequest("수정")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자가 댓글을 삭제하면 지워진다")
    void delete_success() {
        StudyPostComment comment = comment(COMMENT_ID, post(StudyStatus.RECRUITING), user(COMMENTER_ID));
        given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

        commentService.delete(COMMENT_ID, COMMENTER_ID);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("작성자가 아니면 댓글을 삭제할 때 NOT_COMMENT_AUTHOR를 던지고 지우지 않는다")
    void delete_notAuthor() {
        StudyPostComment comment = comment(COMMENT_ID, post(StudyStatus.RECRUITING), user(COMMENTER_ID));
        given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(COMMENT_ID, POST_AUTHOR_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("없는 댓글을 삭제하면 COMMENT_NOT_FOUND를 던진다")
    void delete_notFound() {
        given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(COMMENT_ID, COMMENTER_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    private User user(Long id) {
        User user = User.builder()
                .email("user" + id + "@gmail.com")
                .password("encoded")
                .name("이름")
                .nickname("닉네임" + id)
                .generation(14)
                .part(Part.BE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private StudyPost post(StudyStatus status) {
        StudyPost post = StudyPost.builder().user(user(POST_AUTHOR_ID)).title("제목").content("내용").build();
        ReflectionTestUtils.setField(post, "id", POST_ID);
        post.update(null, null, status);
        return post;
    }

    private StudyPostComment comment(Long id, StudyPost post, User author) {
        StudyPostComment comment = StudyPostComment.builder()
                .studyPost(post)
                .user(author)
                .content("참여하고 싶어요")
                .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
