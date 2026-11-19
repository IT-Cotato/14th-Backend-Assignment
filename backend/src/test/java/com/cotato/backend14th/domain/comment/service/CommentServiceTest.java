package com.cotato.backend14th.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.studypost.service.StudyPostService;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.service.UserService;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private StudyPostCommentRepository commentRepository;
    @Mock
    private StudyPostRepository studyPostRepository;
    @Mock
    private StudyPostService studyPostService;
    @Mock
    private UserService userService;

    @InjectMocks
    private CommentService commentService;

    private static final Long POST_AUTHOR_ID = 1L;
    private static final Long APPLICANT_ID = 2L;

    private User user(Long id) {
        User user = User.builder()
                .email("u" + id + "@gmail.com").password("hashed").name("이름")
                .nickname("닉네임" + id).generation(13).part(Part.FE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private StudyPost post(StudyStatus status) {
        StudyPost post = new StudyPost(user(POST_AUTHOR_ID), "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 10L);
        post.update(null, null, status);
        return post;
    }

    private StudyPostComment comment(Long id, Long writerId) {
        StudyPostComment comment = new StudyPostComment(post(StudyStatus.RECRUITING), user(writerId), "참여하고 싶어요");
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    @Test
    void 모집_중인_글에_댓글로_신청한다() {
        given(studyPostService.getPost(10L)).willReturn(post(StudyStatus.RECRUITING));
        given(userService.getUser(APPLICANT_ID)).willReturn(user(APPLICANT_ID));
        given(commentRepository.save(any(StudyPostComment.class))).willAnswer(invocation -> {
            StudyPostComment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 5L);
            return saved;
        });

        Long commentId = commentService.create(10L, APPLICANT_ID, new CommentRequest("참여하고 싶어요")).commentId();

        assertThat(commentId).isEqualTo(5L);
    }

    @Test
    void 마감된_글에는_댓글을_쓸_수_없다() {
        given(studyPostService.getPost(10L)).willReturn(post(StudyStatus.CLOSED));

        assertThatThrownBy(() -> commentService.create(10L, APPLICANT_ID, new CommentRequest("늦었나요")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STUDY_POST_CLOSED);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void 없는_글에는_댓글을_쓸_수_없다() {
        given(studyPostService.getPost(99L)).willThrow(new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        assertThatThrownBy(() -> commentService.create(99L, APPLICANT_ID, new CommentRequest("안녕하세요")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    void 댓글_목록에서_내_댓글만_isAuthor가_true다() {
        given(studyPostRepository.existsById(10L)).willReturn(true);
        given(commentRepository.findByPostId(10L, PageRequest.of(0, 20))).willReturn(new PageImpl<>(
                List.of(comment(5L, APPLICANT_ID), comment(6L, POST_AUTHOR_ID)), PageRequest.of(0, 20), 2));

        PageResponse<CommentResponse> page = commentService.getComments(10L, APPLICANT_ID, 0, 20);

        assertThat(page.content()).extracting(CommentResponse::isAuthor).containsExactly(true, false);
        assertThat(page.content().get(0).author().generation()).isEqualTo(13);
    }

    @Test
    void 없는_글의_댓글_목록은_STUDY_POST_NOT_FOUND() {
        given(studyPostRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> commentService.getComments(99L, null, 0, 20))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    void 작성자는_댓글을_수정한다() {
        StudyPostComment comment = comment(5L, APPLICANT_ID);
        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));

        commentService.update(5L, APPLICANT_ID, new CommentRequest("수정한 신청 메시지"));

        assertThat(comment.getContent()).isEqualTo("수정한 신청 메시지");
    }

    @Test
    void 남의_댓글은_수정할_수_없다() {
        StudyPostComment comment = comment(5L, APPLICANT_ID);
        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(5L, POST_AUTHOR_ID, new CommentRequest("바꿔치기")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
        assertThat(comment.getContent()).isEqualTo("참여하고 싶어요");
    }

    @Test
    void 남의_댓글은_삭제할_수_없고_없는_댓글은_COMMENT_NOT_FOUND() {
        given(commentRepository.findById(5L)).willReturn(Optional.of(comment(5L, APPLICANT_ID)));
        given(commentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(5L, POST_AUTHOR_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
        assertThatThrownBy(() -> commentService.delete(99L, APPLICANT_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void 작성자는_댓글을_삭제한다() {
        StudyPostComment comment = comment(5L, APPLICANT_ID);
        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));

        commentService.delete(5L, APPLICANT_ID);

        verify(commentRepository).delete(comment);
    }
}
