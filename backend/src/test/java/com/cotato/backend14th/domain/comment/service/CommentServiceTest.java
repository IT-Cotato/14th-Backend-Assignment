package com.cotato.backend14th.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.cotato.backend14th.domain.comment.dto.request.CommentCreateRequest;
import com.cotato.backend14th.domain.comment.dto.request.CommentUpdateRequest;
import com.cotato.backend14th.domain.comment.dto.response.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.response.CommentListItemResponse;
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
class CommentServiceTest {

    @Mock
    private StudyPostCommentRepository studyPostCommentRepository;

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentCreateRequest createRequest;

    @Mock
    private CommentUpdateRequest updateRequest;

    @InjectMocks
    private CommentService commentService;

    private User author(Long id) {
        return new User(id, "author@example.com", "encoded", "홍길동", "길동이", 14, Part.BE);
    }

    private StudyPost studyPost(Long id, User user, StudyStatus status) {
        return new StudyPost(id, user, "제목", "내용", status);
    }

    @Test
    void create_success() {
        User postAuthor = author(1L);
        StudyPost post = studyPost(10L, postAuthor, StudyStatus.RECRUITING);
        User commenter = author(2L);
        when(createRequest.getContent()).thenReturn("신청합니다");
        when(studyPostRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commenter));
        when(studyPostCommentRepository.save(any(StudyPostComment.class)))
                .thenAnswer(invocation -> {
                    StudyPostComment arg = invocation.getArgument(0);
                    return new StudyPostComment(5L, post, commenter, arg.getContent());
                });

        CommentIdResponse response = commentService.create(10L, 2L, createRequest);

        assertThat(response.getCommentId()).isEqualTo(5L);
    }

    @Test
    void create_onClosedPost_throwsStudyPostClosed() {
        StudyPost post = studyPost(10L, author(1L), StudyStatus.CLOSED);
        when(studyPostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.create(10L, 2L, createRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_CLOSED);
    }

    @Test
    void create_onNonexistentPost_throwsStudyPostNotFound() {
        when(studyPostRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(999L, 2L, createRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    void list_success_isAuthorComputed() {
        User commenter = author(2L);
        StudyPostComment comment = new StudyPostComment(5L, studyPost(10L, author(1L), StudyStatus.RECRUITING), commenter, "신청합니다");
        Page<StudyPostComment> page = new PageImpl<>(List.of(comment), PageRequest.of(0, 20), 1);
        when(studyPostRepository.existsById(10L)).thenReturn(true);
        when(studyPostCommentRepository.findByStudyPostIdWithUser(10L, PageRequest.of(0, 20))).thenReturn(page);

        PageResponse<CommentListItemResponse> result = commentService.list(10L, 2L, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getIsAuthor()).isTrue();
    }

    @Test
    void list_isAuthorFalseWhenUserIdNull() {
        User commenter = author(2L);
        StudyPostComment comment = new StudyPostComment(5L, studyPost(10L, author(1L), StudyStatus.RECRUITING), commenter, "신청합니다");
        Page<StudyPostComment> page = new PageImpl<>(List.of(comment), PageRequest.of(0, 20), 1);
        when(studyPostRepository.existsById(10L)).thenReturn(true);
        when(studyPostCommentRepository.findByStudyPostIdWithUser(10L, PageRequest.of(0, 20))).thenReturn(page);

        PageResponse<CommentListItemResponse> result = commentService.list(10L, null, 0, 20);

        assertThat(result.getContent().get(0).getIsAuthor()).isFalse();
    }

    @Test
    void list_onNonexistentPost_throwsStudyPostNotFound() {
        when(studyPostRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.list(999L, 1L, 0, 20))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    void list_negativePage_throwsInvalidInput() {
        assertThatThrownBy(() -> commentService.list(10L, 1L, -1, 20))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void list_zeroSize_throwsInvalidInput() {
        assertThatThrownBy(() -> commentService.list(10L, 1L, 0, 0))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void update_success() {
        User commenter = author(2L);
        StudyPostComment comment = new StudyPostComment(5L, studyPost(10L, author(1L), StudyStatus.RECRUITING), commenter, "원래 내용");
        when(studyPostCommentRepository.findByIdWithUser(5L)).thenReturn(Optional.of(comment));
        when(updateRequest.getContent()).thenReturn("수정한 신청 메시지");

        CommentIdResponse response = commentService.update(5L, 2L, updateRequest);

        assertThat(response.getCommentId()).isEqualTo(5L);
        assertThat(comment.getContent()).isEqualTo("수정한 신청 메시지");
    }

    @Test
    void update_notFound_throwsCommentNotFound() {
        when(studyPostCommentRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(999L, 2L, updateRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    void update_notAuthor_throwsNotCommentAuthor() {
        StudyPostComment comment = new StudyPostComment(5L, studyPost(10L, author(1L), StudyStatus.RECRUITING), author(2L), "내용");
        when(studyPostCommentRepository.findByIdWithUser(5L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(5L, 3L, updateRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
    }

    @Test
    void delete_success() {
        StudyPostComment comment = new StudyPostComment(5L, studyPost(10L, author(1L), StudyStatus.RECRUITING), author(2L), "내용");
        when(studyPostCommentRepository.findByIdWithUser(5L)).thenReturn(Optional.of(comment));

        commentService.delete(5L, 2L);

        verify(studyPostCommentRepository).delete(comment);
    }

    @Test
    void delete_notFound_throwsCommentNotFound() {
        when(studyPostCommentRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(999L, 2L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        verify(studyPostCommentRepository, never()).delete(any());
    }

    @Test
    void delete_notAuthor_throwsNotCommentAuthor() {
        StudyPostComment comment = new StudyPostComment(5L, studyPost(10L, author(1L), StudyStatus.RECRUITING), author(2L), "내용");
        when(studyPostCommentRepository.findByIdWithUser(5L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(5L, 3L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
        verify(studyPostCommentRepository, never()).delete(any());
    }
}
