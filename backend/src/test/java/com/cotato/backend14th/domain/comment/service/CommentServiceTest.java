package com.cotato.backend14th.domain.comment.service;

import com.cotato.backend14th.domain.comment.dto.CommentCreateRequest;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.dto.CommentUpdateRequest;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private StudyPostCommentRepository commentRepository;

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private User postAuthor;
    private User applicant;
    private StudyPost post;
    private StudyPostComment comment;

    @BeforeEach
    void setUp() {
        postAuthor = user(1L, "author@gmail.com", Part.BE);
        applicant = user(2L, "applicant@gmail.com", Part.FE);
        post = new StudyPost(postAuthor, "알고리즘 스터디", "주 2회");
        ReflectionTestUtils.setField(post, "id", 10L);
        comment = new StudyPostComment(post, applicant, "참여하고 싶어요");
        ReflectionTestUtils.setField(comment, "id", 100L);
    }

    // ===== 작성 =====

    @Test
    @DisplayName("모집 중인 글에 댓글을 작성하면 글·작성자·내용이 저장돼요")
    void create() {
        given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));
        given(commentRepository.save(any(StudyPostComment.class))).willAnswer(invocation -> {
            StudyPostComment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        Long commentId = commentService.create(10L, 2L, new CommentCreateRequest("참여할게요")).commentId();

        ArgumentCaptor<StudyPostComment> captor = ArgumentCaptor.forClass(StudyPostComment.class);
        then(commentRepository).should().save(captor.capture());
        assertThat(commentId).isEqualTo(100L);
        assertThat(captor.getValue().getStudyPost()).isSameAs(post);
        assertThat(captor.getValue().getUser()).isSameAs(applicant);
        assertThat(captor.getValue().getContent()).isEqualTo("참여할게요");
    }

    @Test
    @DisplayName("모집이 마감된 글에는 댓글을 작성할 수 없어요 (409)")
    void create_closedPost() {
        post.update(null, null, StudyStatus.CLOSED);
        given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        assertErrorCode(
                () -> commentService.create(10L, 2L, new CommentCreateRequest("늦었나요?")),
                ErrorCode.STUDY_POST_CLOSED
        );
        then(commentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("없는 글에는 댓글을 작성할 수 없어요 (404)")
    void create_postNotFound() {
        given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
        given(studyPostRepository.findById(999L)).willReturn(Optional.empty());

        assertErrorCode(
                () -> commentService.create(999L, 2L, new CommentCreateRequest("내용")),
                ErrorCode.STUDY_POST_NOT_FOUND
        );
    }

    @Test
    @DisplayName("토큰의 회원이 없으면 작성할 수 없어요 (401)")
    void create_userNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertErrorCode(
                () -> commentService.create(10L, 99L, new CommentCreateRequest("내용")),
                ErrorCode.UNAUTHORIZED
        );
        then(commentRepository).should(never()).save(any());
    }

    // ===== 목록 =====

    @Test
    @DisplayName("목록의 isAuthor는 현재 사용자가 쓴 댓글만 true예요")
    void getComments_isAuthor() {
        StudyPostComment authorReply = new StudyPostComment(post, postAuthor, "환영해요");
        ReflectionTestUtils.setField(authorReply, "id", 101L);
        given(studyPostRepository.existsById(10L)).willReturn(true);
        given(commentRepository.findPageByStudyPostId(10L, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(comment, authorReply), PageRequest.of(0, 20), 2));

        PageResponse<CommentResponse> asApplicant = commentService.getComments(10L, 2L, 0, 20);
        PageResponse<CommentResponse> asGuest = commentService.getComments(10L, null, 0, 20);

        assertThat(asApplicant.content()).extracting(CommentResponse::isAuthor).containsExactly(true, false);
        assertThat(asGuest.content()).extracting(CommentResponse::isAuthor).containsExactly(false, false);
        assertThat(asApplicant.content().get(0).author().part()).isEqualTo(Part.FE);
        assertThat(asApplicant.totalElements()).isEqualTo(2);
        assertThat(asApplicant.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("없는 글의 댓글 목록은 404예요")
    void getComments_postNotFound() {
        given(studyPostRepository.existsById(999L)).willReturn(false);

        assertErrorCode(() -> commentService.getComments(999L, null, 0, 20), ErrorCode.STUDY_POST_NOT_FOUND);
        then(commentRepository).should(never()).findPageByStudyPostId(any(), any());
    }

    // ===== 수정 =====

    @Test
    @DisplayName("작성자는 댓글을 수정할 수 있어요")
    void update() {
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        Long commentId = commentService.update(100L, 2L, new CommentUpdateRequest("수정했어요")).commentId();

        assertThat(commentId).isEqualTo(100L);
        assertThat(comment.getContent()).isEqualTo("수정했어요");
    }

    @Test
    @DisplayName("작성자가 아니면 수정할 수 없어요 (403). 모집글 작성자여도 마찬가지예요")
    void update_notAuthor() {
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        assertErrorCode(
                () -> commentService.update(100L, 1L, new CommentUpdateRequest("가로채기")),
                ErrorCode.NOT_COMMENT_AUTHOR
        );
        assertThat(comment.getContent()).isEqualTo("참여하고 싶어요");
    }

    @Test
    @DisplayName("없는 댓글은 수정·삭제할 수 없어요 (404)")
    void updateAndDelete_notFound() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertErrorCode(
                () -> commentService.update(999L, 2L, new CommentUpdateRequest("x")),
                ErrorCode.COMMENT_NOT_FOUND
        );
        assertErrorCode(() -> commentService.delete(999L, 2L), ErrorCode.COMMENT_NOT_FOUND);
    }

    // ===== 삭제 =====

    @Test
    @DisplayName("작성자는 삭제할 수 있고, 다른 사람은 403이에요")
    void delete() {
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        assertErrorCode(() -> commentService.delete(100L, 1L), ErrorCode.NOT_COMMENT_AUTHOR);
        then(commentRepository).should(never()).delete(any());

        commentService.delete(100L, 2L);
        then(commentRepository).should().delete(comment);
    }

    private User user(Long id, String email, Part part) {
        User user = new User(email, "encoded", "이름", "닉네임" + id, 14, part);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void assertErrorCode(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
