package com.cotato.backend14th.domain.comment.service;

import static com.cotato.backend14th.support.TestFixtures.comment;
import static com.cotato.backend14th.support.TestFixtures.commentRequest;
import static com.cotato.backend14th.support.TestFixtures.studyPost;
import static com.cotato.backend14th.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

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

import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.CommentRepository;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    // ---------- 작성 ----------

    @Test
    @DisplayName("댓글을 작성하면 모집글과 작성자를 연결해서 저장한다")
    void create() {
        // given
        StudyPost post = studyPost(10L, user(1L));
        User applicant = user(2L);
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
        given(commentRepository.save(any(StudyPostComment.class))).willAnswer(invocation -> {
            StudyPostComment saved = invocation.getArgument(0);
            setField(saved, "id", 100L);
            return saved;
        });

        // when
        CommentIdResponse response = commentService.create(10L, 2L, commentRequest("참여하고 싶어요!"));

        // then
        assertThat(response.getCommentId()).isEqualTo(100L);

        ArgumentCaptor<StudyPostComment> captor = ArgumentCaptor.forClass(StudyPostComment.class);
        then(commentRepository).should().save(captor.capture());
        StudyPostComment saved = captor.getValue();
        assertThat(saved.getStudyPost()).isSameAs(post);
        assertThat(saved.getUser()).isSameAs(applicant);
        assertThat(saved.getContent()).isEqualTo("참여하고 싶어요!");
    }

    @Test
    @DisplayName("모집이 마감된 글에 댓글을 쓰면 STUDY_POST_CLOSED 예외가 나고 저장하지 않는다")
    void createOnClosedPost() {
        // given
        StudyPost post = studyPost(10L, user(1L));
        post.update(null, null, StudyStatus.CLOSED);
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> commentService.create(10L, 2L, commentRequest("늦었지만 신청할래요.")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_CLOSED);

        then(commentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("없는 모집글에 댓글을 쓰면 STUDY_POST_NOT_FOUND 예외가 난다")
    void createOnUnknownPost() {
        // given
        given(studyPostRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.create(99L, 2L, commentRequest("참여하고 싶어요!")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);

        then(commentRepository).should(never()).save(any());
    }

    // ---------- 목록 ----------

    @Test
    @DisplayName("댓글 목록의 isAuthor는 내 댓글이면 true, 남의 댓글이면 false다")
    void getListIsAuthor() {
        // given
        StudyPost post = studyPost(10L, user(1L));
        StudyPostComment mine = comment(100L, post, user(2L), "제 댓글이에요.");
        StudyPostComment others = comment(101L, post, user(3L), "다른 사람 댓글이에요.");
        given(studyPostRepository.existsById(10L)).willReturn(true);
        given(commentRepository.findPageByPostId(10L, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(mine, others), PageRequest.of(0, 20), 2));

        // when
        PageResponse<CommentResponse> response = commentService.getList(10L, 2L, 0, 20);

        // then
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent())
                .extracting("commentId", "isAuthor")
                .containsExactly(tuple(100L, true), tuple(101L, false));
    }

    @Test
    @DisplayName("없는 모집글의 댓글 목록을 조회하면 STUDY_POST_NOT_FOUND 예외가 난다")
    void getListOnUnknownPost() {
        // given
        given(studyPostRepository.existsById(99L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> commentService.getList(99L, null, 0, 20))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);

        then(commentRepository).should(never()).findPageByPostId(any(), any());
    }

    @Test
    @DisplayName("댓글 목록 크기가 100을 넘으면 INVALID_INPUT 예외가 나고 조회하지 않는다")
    void getListWithInvalidSize() {
        // when & then
        assertThatThrownBy(() -> commentService.getList(10L, null, 0, 101))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verifyNoInteractions(studyPostRepository, commentRepository);
    }

    // ---------- 수정 ----------

    @Test
    @DisplayName("작성자가 댓글을 수정하면 내용이 바뀐다")
    void update() {
        // given
        StudyPostComment comment = comment(100L, studyPost(10L, user(1L)), user(2L), "처음 쓴 댓글");
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        // when
        CommentIdResponse response = commentService.update(100L, 2L, commentRequest("고친 댓글"));

        // then
        assertThat(response.getCommentId()).isEqualTo(100L);
        assertThat(comment.getContent()).isEqualTo("고친 댓글");
    }

    @Test
    @DisplayName("작성자가 아닌 사람이 댓글을 수정하면 NOT_COMMENT_AUTHOR 예외가 나고 내용은 그대로다")
    void updateByOtherUser() {
        // given
        StudyPostComment comment = comment(100L, studyPost(10L, user(1L)), user(2L), "처음 쓴 댓글");
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.update(100L, 3L, commentRequest("몰래 바꾼 댓글")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);

        assertThat(comment.getContent()).isEqualTo("처음 쓴 댓글");
    }

    // ---------- 삭제 ----------

    @Test
    @DisplayName("작성자가 댓글을 삭제하면 지운다")
    void delete() {
        // given
        StudyPostComment comment = comment(100L, studyPost(10L, user(1L)), user(2L), "지울 댓글");
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        // when
        commentService.delete(100L, 2L);

        // then
        then(commentRepository).should().delete(comment);
    }

    @Test
    @DisplayName("작성자가 아닌 사람이 댓글을 삭제하면 NOT_COMMENT_AUTHOR 예외가 나고 지우지 않는다")
    void deleteByOtherUser() {
        // given
        StudyPostComment comment = comment(100L, studyPost(10L, user(1L)), user(2L), "지울 댓글");
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.delete(100L, 3L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);

        then(commentRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("없는 댓글을 수정하거나 삭제하면 COMMENT_NOT_FOUND 예외가 난다")
    void updateOrDeleteUnknownComment() {
        // given
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.update(999L, 2L, commentRequest("고친 댓글")))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        assertThatThrownBy(() -> commentService.delete(999L, 2L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

        then(commentRepository).should(never()).delete(any());
    }
}
