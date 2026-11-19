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
import org.springframework.data.domain.Pageable;

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
import com.cotato.backend14th.global.common.dto.PageResponse;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock StudyPostCommentRepository commentRepository;
    @Mock StudyPostRepository studyPostRepository;
    @Mock UserService userService;
    @InjectMocks CommentService commentService;

    private final User postAuthor = TestFixtures.user(1L, Part.BE);
    private final User applicant = TestFixtures.user(2L, Part.FE);

    @Test
    @DisplayName("모집 중인 글에는 댓글(신청)을 쓸 수 있다")
    void create_success() {
        StudyPost post = TestFixtures.post(10L, postAuthor);
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));
        given(userService.getUser(2L)).willReturn(applicant);
        given(commentRepository.save(any(StudyPostComment.class)))
                .willAnswer(inv -> inv.getArgument(0));

        commentService.create(10L, 2L, new CommentRequest("참여하고 싶어요"));

        verify(commentRepository).save(any(StudyPostComment.class));
    }

    @Test
    @DisplayName("마감된(CLOSED) 글에 쓰면 STUDY_POST_CLOSED")
    void create_closedPost() {
        StudyPost post = TestFixtures.post(10L, postAuthor);
        post.update(null, null, StudyStatus.CLOSED);
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.create(10L, 2L, new CommentRequest("늦었나요")))
                .extracting("errorCode").isEqualTo(ErrorCode.STUDY_POST_CLOSED);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 글에 쓰면 STUDY_POST_NOT_FOUND")
    void create_postNotFound() {
        given(studyPostRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(999L, 2L, new CommentRequest("x")))
                .extracting("errorCode").isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("목록은 로그인한 사람 기준으로 isAuthor를 채운다")
    void getComments_isAuthor() {
        StudyPost post = TestFixtures.post(10L, postAuthor);
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostId(eq(10L), any(Pageable.class))).willReturn(new PageImpl<>(List.of(
                TestFixtures.comment(1L, post, applicant), TestFixtures.comment(2L, post, postAuthor))));

        PageResponse<CommentResponse> result = commentService.getComments(10L, 2L, 0, 20);

        assertThat(result.content()).extracting(CommentResponse::isAuthor).containsExactly(true, false);
    }

    @Test
    @DisplayName("작성자는 댓글을 수정할 수 있다")
    void update_success() {
        StudyPostComment comment = TestFixtures.comment(5L, TestFixtures.post(10L, postAuthor), applicant);
        given(commentRepository.findWithUserById(5L)).willReturn(Optional.of(comment));

        commentService.update(5L, 2L, new CommentRequest("수정한 신청 메시지"));

        assertThat(comment.getContent()).isEqualTo("수정한 신청 메시지");
    }

    @Test
    @DisplayName("남의 댓글은 수정할 수 없다 (NOT_COMMENT_AUTHOR)")
    void update_notAuthor() {
        StudyPostComment comment = TestFixtures.comment(5L, TestFixtures.post(10L, postAuthor), applicant);
        given(commentRepository.findWithUserById(5L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(5L, 1L, new CommentRequest("해킹")))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
        assertThat(comment.getContent()).isEqualTo("댓글5");
    }

    @Test
    @DisplayName("남의 댓글은 삭제할 수 없다 (NOT_COMMENT_AUTHOR)")
    void delete_notAuthor() {
        StudyPostComment comment = TestFixtures.comment(5L, TestFixtures.post(10L, postAuthor), applicant);
        given(commentRepository.findWithUserById(5L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(5L, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND")
    void delete_notFound() {
        given(commentRepository.findWithUserById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(999L, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }
}
