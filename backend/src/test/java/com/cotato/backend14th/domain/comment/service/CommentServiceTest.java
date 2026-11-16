package com.cotato.backend14th.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.study.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long COMMENT_ID = 100L;

    @Mock
    private StudyPostCommentRepository studyPostCommentRepository;

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    @Nested
    @DisplayName("댓글 작성")
    class Create {

        @Test
        @DisplayName("모집 중인 글이면 댓글을 저장하고 댓글 id를 반환한다")
        void create_success() {
            // given
            User user = savedUser(AUTHOR_ID);
            StudyPost post = savedPost(POST_ID, savedUser(OTHER_USER_ID));
            given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(user));
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));
            given(studyPostCommentRepository.save(any(StudyPostComment.class))).willAnswer(invocation -> {
                StudyPostComment saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", COMMENT_ID);
                return saved;
            });

            // when
            CommentIdResponse response = commentService.create(POST_ID, AUTHOR_ID, commentRequest("참여하고 싶어요"));

            // then
            ArgumentCaptor<StudyPostComment> captor = ArgumentCaptor.forClass(StudyPostComment.class);
            verify(studyPostCommentRepository).save(captor.capture());
            StudyPostComment saved = captor.getValue();

            assertThat(saved.getContent()).isEqualTo("참여하고 싶어요");
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getStudyPost()).isEqualTo(post);
            assertThat(response.getCommentId()).isEqualTo(COMMENT_ID);
        }

        @Test
        @DisplayName("마감된 글이면 STUDY_POST_CLOSED 예외가 발생하고 저장하지 않는다")
        void create_closedPost() {
            // given
            StudyPost post = savedPost(POST_ID, savedUser(OTHER_USER_ID));
            post.update(null, null, StudyStatus.CLOSED);
            given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(savedUser(AUTHOR_ID)));
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> commentService.create(POST_ID, AUTHOR_ID, commentRequest("참여하고 싶어요")))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STUDY_POST_CLOSED);

            verify(studyPostCommentRepository, never()).save(any());
        }

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void create_postNotFound() {
            // given
            given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(savedUser(AUTHOR_ID)));
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.create(POST_ID, AUTHOR_ID, commentRequest("참여하고 싶어요")))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);

            verify(studyPostCommentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class Update {

        @Test
        @DisplayName("작성자면 내용을 수정하고 댓글 id를 반환한다")
        void update_byAuthor() {
            // given
            StudyPostComment comment = savedComment(COMMENT_ID, savedUser(AUTHOR_ID));
            given(studyPostCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            // when
            CommentIdResponse response = commentService.update(COMMENT_ID, AUTHOR_ID, commentRequest("수정된 댓글"));

            // then
            assertThat(comment.getContent()).isEqualTo("수정된 댓글");
            assertThat(response.getCommentId()).isEqualTo(COMMENT_ID);
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_COMMENT_AUTHOR 예외가 발생하고 내용은 바뀌지 않는다")
        void update_byOtherUser() {
            // given
            StudyPostComment comment = savedComment(COMMENT_ID, savedUser(AUTHOR_ID));
            given(studyPostCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.update(COMMENT_ID, OTHER_USER_ID, commentRequest("수정된 댓글")))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);

            assertThat(comment.getContent()).isEqualTo("원래 댓글");
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND 예외가 발생한다")
        void update_commentNotFound() {
            // given
            given(studyPostCommentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.update(COMMENT_ID, AUTHOR_ID, commentRequest("수정된 댓글")))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class Delete {

        @Test
        @DisplayName("작성자면 댓글을 삭제한다")
        void delete_byAuthor() {
            // given
            StudyPostComment comment = savedComment(COMMENT_ID, savedUser(AUTHOR_ID));
            given(studyPostCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            // when
            commentService.delete(COMMENT_ID, AUTHOR_ID);

            // then
            verify(studyPostCommentRepository).delete(comment);
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_COMMENT_AUTHOR 예외가 발생하고 삭제하지 않는다")
        void delete_byOtherUser() {
            // given
            StudyPostComment comment = savedComment(COMMENT_ID, savedUser(AUTHOR_ID));
            given(studyPostCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.delete(COMMENT_ID, OTHER_USER_ID))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);

            verify(studyPostCommentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND 예외가 발생하고 삭제하지 않는다")
        void delete_commentNotFound() {
            // given
            given(studyPostCommentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.delete(COMMENT_ID, AUTHOR_ID))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

            verify(studyPostCommentRepository, never()).delete(any());
        }
    }

    // 엔티티와 요청 DTO에 setter가 없어서 리플렉션으로 필드를 채워요
    private User savedUser(Long id) {
        User user = User.create("user" + id + "@gmail.com", "encoded-password", "홍길동", "길동이" + id, 14, Part.BE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private StudyPost savedPost(Long id, User author) {
        StudyPost post = StudyPost.create("원래 제목", "원래 내용", author);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private StudyPostComment savedComment(Long id, User author) {
        StudyPostComment comment = StudyPostComment.create("원래 댓글", author, savedPost(POST_ID, author));
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private CommentRequest commentRequest(String content) {
        CommentRequest request = new CommentRequest();
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }
}
