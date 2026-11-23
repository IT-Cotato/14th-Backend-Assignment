package com.cotato.backend14th.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
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

    @Mock
    private StudyPostCommentRepository commentRepository;
    @Mock
    private StudyPostRepository studyPostRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyPostCommentService commentService;

    private static final Long POST_AUTHOR_ID = 1L;
    private static final Long APPLICANT_ID = 2L;
    private static final Long OTHER_ID = 3L;
    private static final Long POST_ID = 10L;
    private static final Long COMMENT_ID = 100L;

    private static User user(Long id, String nickname, Part part) {
        User user = User.builder()
                .email(nickname + "@gmail.com")
                .password("$2a$10$encoded")
                .name("이름")
                .nickname(nickname)
                .generation(14)
                .part(part)
                .build();
        // id는 DB가 채우는 값이라 테스트에서만 직접 넣어요
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static StudyPost post(StudyStatus status) {
        StudyPost post = StudyPost.builder()
                .user(user(POST_AUTHOR_ID, "글쓴이", Part.BE))
                .title("알고리즘 스터디 모집")
                .content("주 2회 백준 풀이")
                .build();
        ReflectionTestUtils.setField(post, "id", POST_ID);
        post.update(null, null, status);
        return post;
    }

    private static StudyPostComment comment(Long id, User author, String content) {
        StudyPostComment comment = StudyPostComment.builder()
                .studyPost(post(StudyStatus.RECRUITING))
                .user(author)
                .content(content)
                .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private static void assertErrorCode(Throwable thrown, ErrorCode expected) {
        assertThat(thrown).isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(expected);
    }

    @Nested
    @DisplayName("작성")
    class Create {

        @Test
        @DisplayName("모집 중인 글에 글·작성자·내용을 지정해 저장하고 commentId를 돌려줘요")
        void success() {
            StudyPost post = post(StudyStatus.RECRUITING);
            User applicant = user(APPLICANT_ID, "신청자", Part.FE);
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));
            given(userRepository.findById(APPLICANT_ID)).willReturn(Optional.of(applicant));
            given(commentRepository.save(any(StudyPostComment.class))).willAnswer(invocation -> {
                StudyPostComment saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", COMMENT_ID);
                return saved;
            });

            CommentIdResponse response = commentService.create(
                    POST_ID, APPLICANT_ID, new CommentRequest("참여하고 싶어요"));

            assertThat(response.getCommentId()).isEqualTo(COMMENT_ID);
            ArgumentCaptor<StudyPostComment> captor = ArgumentCaptor.forClass(StudyPostComment.class);
            verify(commentRepository).save(captor.capture());
            StudyPostComment saved = captor.getValue();
            assertThat(saved.getStudyPost()).isSameAs(post);
            assertThat(saved.getUser()).isSameAs(applicant);
            assertThat(saved.getContent()).isEqualTo("참여하고 싶어요");
        }

        @Test
        @DisplayName("마감된 글이면 STUDY_POST_CLOSED를 던지고 저장하지 않아요")
        void closedPost() {
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post(StudyStatus.CLOSED)));

            assertThatThrownBy(() -> commentService.create(POST_ID, APPLICANT_ID, new CommentRequest("늦었지만 신청")))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.STUDY_POST_CLOSED));
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND를 던져요")
        void postNotFound() {
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.create(POST_ID, APPLICANT_ID, new CommentRequest("신청")))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.STUDY_POST_NOT_FOUND));
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("토큰의 유저가 없으면 UNAUTHORIZED를 던져요")
        void userNotFound() {
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post(StudyStatus.RECRUITING)));
            given(userRepository.findById(APPLICANT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.create(POST_ID, APPLICANT_ID, new CommentRequest("신청")))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.UNAUTHORIZED));
            verify(commentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("목록")
    class GetList {

        private void givenTwoComments() {
            given(studyPostRepository.existsById(POST_ID)).willReturn(true);
            given(commentRepository.findPageByStudyPostId(POST_ID, PageRequest.of(0, 20)))
                    .willReturn(new PageImpl<>(List.of(
                            comment(1L, user(APPLICANT_ID, "신청자", Part.FE), "첫 신청"),
                            comment(2L, user(OTHER_ID, "다른이", Part.DE), "두 번째 신청")
                    ), PageRequest.of(0, 20), 2));
        }

        @Test
        @DisplayName("페이지를 그대로 넘기고, 로그인한 사람의 댓글만 isAuthor가 true예요")
        void isAuthorForLoginUser() {
            givenTwoComments();

            PageResponse<CommentResponse> response = commentService.getList(POST_ID, APPLICANT_ID, 0, 20);

            assertThat(response.getTotalElements()).isEqualTo(2);
            assertThat(response.getSize()).isEqualTo(20);
            assertThat(response.getContent())
                    .extracting(CommentResponse::getCommentId, CommentResponse::getIsAuthor)
                    .containsExactly(tuple(1L, true), tuple(2L, false));
            assertThat(response.getContent().get(0).getAuthor().getNickname()).isEqualTo("신청자");
        }

        @Test
        @DisplayName("로그인하지 않았으면(null) 모든 댓글의 isAuthor가 false예요")
        void anonymous() {
            givenTwoComments();

            PageResponse<CommentResponse> response = commentService.getList(POST_ID, null, 0, 20);

            assertThat(response.getContent()).extracting(CommentResponse::getIsAuthor).containsOnly(false);
        }

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND를 던지고 댓글을 조회하지 않아요")
        void postNotFound() {
            given(studyPostRepository.existsById(POST_ID)).willReturn(false);

            assertThatThrownBy(() -> commentService.getList(POST_ID, null, 0, 20))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.STUDY_POST_NOT_FOUND));
            verify(commentRepository, never()).findPageByStudyPostId(any(), any());
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("작성자면 내용을 바꾸고 commentId를 돌려줘요")
        void success() {
            StudyPostComment comment = comment(COMMENT_ID, user(APPLICANT_ID, "신청자", Part.FE), "처음 내용");
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

            CommentIdResponse response = commentService.update(
                    COMMENT_ID, APPLICANT_ID, new CommentRequest("수정한 신청 메시지"));

            assertThat(response.getCommentId()).isEqualTo(COMMENT_ID);
            assertThat(comment.getContent()).isEqualTo("수정한 신청 메시지");
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_COMMENT_AUTHOR를 던지고 내용은 그대로예요")
        void notAuthor() {
            StudyPostComment comment = comment(COMMENT_ID, user(APPLICANT_ID, "신청자", Part.FE), "처음 내용");
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.update(COMMENT_ID, OTHER_ID, new CommentRequest("남이 고친 내용")))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.NOT_COMMENT_AUTHOR));
            assertThat(comment.getContent()).isEqualTo("처음 내용");
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND를 던져요")
        void notFound() {
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.update(COMMENT_ID, APPLICANT_ID, new CommentRequest("내용")))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.COMMENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("작성자면 댓글을 삭제해요")
        void success() {
            StudyPostComment comment = comment(COMMENT_ID, user(APPLICANT_ID, "신청자", Part.FE), "내용");
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.of(comment));

            commentService.delete(COMMENT_ID, APPLICANT_ID);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_COMMENT_AUTHOR를 던지고 삭제하지 않아요")
        void notAuthor() {
            given(commentRepository.findWithUserById(COMMENT_ID))
                    .willReturn(Optional.of(comment(COMMENT_ID, user(APPLICANT_ID, "신청자", Part.FE), "내용")));

            assertThatThrownBy(() -> commentService.delete(COMMENT_ID, OTHER_ID))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.NOT_COMMENT_AUTHOR));
            verify(commentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND를 던지고 삭제하지 않아요")
        void notFound() {
            given(commentRepository.findWithUserById(COMMENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.delete(COMMENT_ID, APPLICANT_ID))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.COMMENT_NOT_FOUND));
            verify(commentRepository, never()).delete(any());
        }
    }
}
