package com.cotato.backend14th.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.study.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final Long POST_AUTHOR_ID = 1L;
    private static final Long COMMENTER_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;
    private static final Long POST_ID = 10L;
    private static final Long COMMENT_ID = 100L;

    @Mock
    private StudyPostCommentRepository commentRepository;

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private static void assertErrorCode(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(Backend14thException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private static User userWithId(Long id) {
        User user = User.create("user" + id + "@example.com", "encoded", "tester", "nick" + id, 14, Part.BE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static StudyPost postWithStatus(StudyStatus status) {
        StudyPost post = StudyPost.create(userWithId(POST_AUTHOR_ID), "제목", "내용");
        ReflectionTestUtils.setField(post, "id", POST_ID);
        post.update(null, null, status);
        return post;
    }

    private static StudyPostComment commentBy(User author) {
        StudyPostComment comment = StudyPostComment.create(postWithStatus(StudyStatus.RECRUITING), author, "원래 신청 메시지");
        ReflectionTestUtils.setField(comment, "id", COMMENT_ID);
        return comment;
    }

    @Nested
    @DisplayName("댓글(신청) 작성")
    class Create {

        private final CommentRequest request = new CommentRequest("참여하고 싶어요");

        @Test
        @DisplayName("모집 중인 글에는 댓글을 저장하고 댓글 ID를 반환한다")
        void savesComment() {
            StudyPost post = postWithStatus(StudyStatus.RECRUITING);
            User commenter = userWithId(COMMENTER_ID);
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));
            given(userRepository.findById(COMMENTER_ID)).willReturn(Optional.of(commenter));
            given(commentRepository.save(any(StudyPostComment.class))).willAnswer(invocation -> {
                StudyPostComment saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", COMMENT_ID);
                return saved;
            });

            CommentIdResponse response = commentService.create(POST_ID, COMMENTER_ID, request);

            assertThat(response.getCommentId()).isEqualTo(COMMENT_ID);
            ArgumentCaptor<StudyPostComment> captor = ArgumentCaptor.forClass(StudyPostComment.class);
            verify(commentRepository).save(captor.capture());
            assertThat(captor.getValue().getStudyPost()).isSameAs(post);
            assertThat(captor.getValue().getUser()).isSameAs(commenter);
            assertThat(captor.getValue().getContent()).isEqualTo("참여하고 싶어요");
        }

        @Test
        @DisplayName("모집이 마감된 글에는 STUDY_POST_CLOSED이고 저장하지 않는다")
        void rejectsClosedPost() {
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(postWithStatus(StudyStatus.CLOSED)));

            assertErrorCode(() -> commentService.create(POST_ID, COMMENTER_ID, request), ErrorCode.STUDY_POST_CLOSED);

            verify(commentRepository, never()).save(any(StudyPostComment.class));
        }

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND이다")
        void rejectsMissingPost() {
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.empty());

            assertErrorCode(() -> commentService.create(POST_ID, COMMENTER_ID, request), ErrorCode.STUDY_POST_NOT_FOUND);
        }

        @Test
        @DisplayName("토큰의 사용자가 존재하지 않으면 UNAUTHORIZED이다")
        void rejectsUnknownUser() {
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(postWithStatus(StudyStatus.RECRUITING)));
            given(userRepository.findById(COMMENTER_ID)).willReturn(Optional.empty());

            assertErrorCode(() -> commentService.create(POST_ID, COMMENTER_ID, request), ErrorCode.UNAUTHORIZED);

            verify(commentRepository, never()).save(any(StudyPostComment.class));
        }

        @Test
        @DisplayName("같은 사용자가 같은 글에 댓글을 여러 번 달 수 있다")
        void allowsMultipleCommentsFromSameUser() {
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(postWithStatus(StudyStatus.RECRUITING)));
            given(userRepository.findById(COMMENTER_ID)).willReturn(Optional.of(userWithId(COMMENTER_ID)));
            given(commentRepository.save(any(StudyPostComment.class))).willAnswer(invocation -> invocation.getArgument(0));

            commentService.create(POST_ID, COMMENTER_ID, request);
            commentService.create(POST_ID, COMMENTER_ID, request);

            verify(commentRepository, org.mockito.Mockito.times(2)).save(any(StudyPostComment.class));
        }
    }

    @Nested
    @DisplayName("댓글 목록")
    class GetList {

        @Test
        @DisplayName("작성자 정보와 함께 반환하고 본인 댓글만 isAuthor가 true이다")
        void marksOwnComments() {
            StudyPostComment mine = commentBy(userWithId(COMMENTER_ID));
            StudyPostComment others = commentBy(userWithId(OTHER_USER_ID));
            ReflectionTestUtils.setField(others, "id", COMMENT_ID + 1);
            given(studyPostRepository.existsById(POST_ID)).willReturn(true);
            given(commentRepository.findByPostId(any(Long.class), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(mine, others), PageRequest.of(0, 20), 2));

            PageResponse<CommentResponse> response = commentService.getList(POST_ID, COMMENTER_ID, 0, 20);

            assertThat(response.getTotalElements()).isEqualTo(2);
            assertThat(response.getContent()).extracting(CommentResponse::isMine).containsExactly(true, false);
            assertThat(response.getContent().get(0).getAuthor().getNickname()).isEqualTo("nick" + COMMENTER_ID);
        }

        @Test
        @DisplayName("로그인하지 않으면(null) 모든 댓글의 isAuthor가 false이다")
        void anonymousSeesNoOwnComments() {
            given(studyPostRepository.existsById(POST_ID)).willReturn(true);
            given(commentRepository.findByPostId(any(Long.class), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(commentBy(userWithId(COMMENTER_ID)))));

            PageResponse<CommentResponse> response = commentService.getList(POST_ID, null, 0, 20);

            assertThat(response.getContent()).extracting(CommentResponse::isMine).containsExactly(false);
        }

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND이다")
        void rejectsMissingPost() {
            given(studyPostRepository.existsById(POST_ID)).willReturn(false);

            assertErrorCode(() -> commentService.getList(POST_ID, null, 0, 20), ErrorCode.STUDY_POST_NOT_FOUND);

            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("page가 음수이거나 size가 1~100을 벗어나면 INVALID_INPUT이다")
        void rejectsInvalidPaging() {
            assertErrorCode(() -> commentService.getList(POST_ID, null, -1, 20), ErrorCode.INVALID_INPUT);
            assertErrorCode(() -> commentService.getList(POST_ID, null, 0, 0), ErrorCode.INVALID_INPUT);
            assertErrorCode(() -> commentService.getList(POST_ID, null, 0, 101), ErrorCode.INVALID_INPUT);

            verifyNoInteractions(studyPostRepository, commentRepository);
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class Update {

        @Test
        @DisplayName("작성자는 내용을 수정할 수 있다")
        void authorCanUpdate() {
            StudyPostComment comment = commentBy(userWithId(COMMENTER_ID));
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

            CommentIdResponse response = commentService.update(COMMENT_ID, COMMENTER_ID, new CommentRequest("수정한 메시지"));

            assertThat(response.getCommentId()).isEqualTo(COMMENT_ID);
            assertThat(comment.getContent()).isEqualTo("수정한 메시지");
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_COMMENT_AUTHOR이고 내용은 바뀌지 않는다")
        void rejectsNonAuthor() {
            StudyPostComment comment = commentBy(userWithId(COMMENTER_ID));
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

            assertErrorCode(() -> commentService.update(COMMENT_ID, OTHER_USER_ID, new CommentRequest("해킹")),
                    ErrorCode.NOT_COMMENT_AUTHOR);

            assertThat(comment.getContent()).isEqualTo("원래 신청 메시지");
        }

        @Test
        @DisplayName("글 작성자라도 다른 사람의 댓글은 수정할 수 없다")
        void postAuthorCannotEditOthersComment() {
            StudyPostComment comment = commentBy(userWithId(COMMENTER_ID));
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

            assertErrorCode(() -> commentService.update(COMMENT_ID, POST_AUTHOR_ID, new CommentRequest("수정")),
                    ErrorCode.NOT_COMMENT_AUTHOR);
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND이다")
        void rejectsMissingComment() {
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.empty());

            assertErrorCode(() -> commentService.update(COMMENT_ID, COMMENTER_ID, new CommentRequest("수정")),
                    ErrorCode.COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("모집이 마감된 글의 댓글도 작성자는 수정할 수 있다")
        void canUpdateOnClosedPost() {
            StudyPost closed = postWithStatus(StudyStatus.CLOSED);
            StudyPostComment comment = StudyPostComment.create(closed, userWithId(COMMENTER_ID), "원래 신청 메시지");
            ReflectionTestUtils.setField(comment, "id", COMMENT_ID);
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

            commentService.update(COMMENT_ID, COMMENTER_ID, new CommentRequest("마감 후 수정"));

            assertThat(comment.getContent()).isEqualTo("마감 후 수정");
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class Delete {

        @Test
        @DisplayName("작성자는 삭제할 수 있다")
        void authorCanDelete() {
            StudyPostComment comment = commentBy(userWithId(COMMENTER_ID));
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

            commentService.delete(COMMENT_ID, COMMENTER_ID);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_COMMENT_AUTHOR이고 삭제하지 않는다")
        void rejectsNonAuthor() {
            given(commentRepository.findWithUserById(COMMENT_ID))
                    .willReturn(Optional.of(commentBy(userWithId(COMMENTER_ID))));

            assertErrorCode(() -> commentService.delete(COMMENT_ID, OTHER_USER_ID), ErrorCode.NOT_COMMENT_AUTHOR);

            verify(commentRepository, never()).delete(any(StudyPostComment.class));
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND이다")
        void rejectsMissingComment() {
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.empty());

            assertErrorCode(() -> commentService.delete(COMMENT_ID, COMMENTER_ID), ErrorCode.COMMENT_NOT_FOUND);

            verify(commentRepository, never()).delete(any(StudyPostComment.class));
        }
    }
}
