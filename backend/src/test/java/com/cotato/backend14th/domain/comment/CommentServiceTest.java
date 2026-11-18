package com.cotato.backend14th.domain.comment;

import static com.cotato.backend14th.support.Fixtures.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
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

import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.comment.service.CommentService;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.support.Fixtures;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    StudyPostCommentRepository commentRepository;
    @Mock
    StudyPostRepository studyPostRepository;
    @Mock
    UserRepository userRepository;
    @InjectMocks
    CommentService service;

    private final User postAuthor = Fixtures.user(1L, "postauthor", Part.BE);
    private final User writer = Fixtures.user(2L, "writer", Part.FE);
    private final User stranger = Fixtures.user(3L, "stranger", Part.PM);
    private final StudyPost post = Fixtures.post(10L, postAuthor, "모집글");

    private CommentRequest request(String content) {
        return Fixtures.request(CommentRequest.class, Map.of("content", content));
    }

    @Test
    @DisplayName("모집 중인 글에는 댓글을 쓸 수 있고, 같은 사람이 여러 번 써도 된다")
    void create() {
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(writer));
        given(commentRepository.save(any(StudyPostComment.class))).willAnswer(invocation -> {
            StudyPostComment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 5L);
            return saved;
        });

        CommentIdResponse first = service.create(10L, 2L, request("참여하고 싶어요"));
        service.create(10L, 2L, request("한 번 더 남겨요"));

        assertThat(first.getCommentId()).isEqualTo(5L);
        ArgumentCaptor<StudyPostComment> captor = ArgumentCaptor.forClass(StudyPostComment.class);
        verify(commentRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStudyPost()).isSameAs(post);
        assertThat(captor.getAllValues().get(0).getUser()).isSameAs(writer);
        assertThat(captor.getAllValues().get(0).getContent()).isEqualTo("참여하고 싶어요");
    }

    @Test
    @DisplayName("없는 글에 댓글을 쓰면 STUDY_POST_NOT_FOUND이다")
    void createOnMissingPost() {
        given(studyPostRepository.findById(99L)).willReturn(Optional.empty());

        assertErrorCode(() -> service.create(99L, 2L, request("x")), ErrorCode.STUDY_POST_NOT_FOUND);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("모집이 마감된 글에는 댓글을 쓸 수 없다 (STUDY_POST_CLOSED)")
    void createOnClosedPost() {
        StudyPostUpdateRequest close =
                Fixtures.request(StudyPostUpdateRequest.class, Map.of("studyStatus", StudyStatus.CLOSED));
        post.update(close.getTitle(), close.getContent(), close.getStudyStatus());
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        assertErrorCode(() -> service.create(10L, 2L, request("x")), ErrorCode.STUDY_POST_CLOSED);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("토큰의 사용자가 DB에 없으면 UNAUTHORIZED이다")
    void createWithUnknownUser() {
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertErrorCode(() -> service.create(10L, 99L, request("x")), ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("댓글 목록의 isAuthor는 내가 쓴 댓글에만 true이다")
    void listIsAuthor() {
        StudyPostComment mine = Fixtures.comment(1L, post, writer, "내 댓글");
        StudyPostComment others = Fixtures.comment(2L, post, stranger, "남의 댓글");
        given(studyPostRepository.existsById(10L)).willReturn(true);
        given(commentRepository.findByPostId(org.mockito.ArgumentMatchers.eq(10L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(mine, others), PageRequest.of(0, 20), 2));

        PageResponse<CommentResponse> response = service.getList(10L, 2L, 0, 20);

        assertThat(response.getContent()).extracting(CommentResponse::getIsAuthor).containsExactly(true, false);
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("로그인하지 않으면 모든 댓글의 isAuthor가 false이다")
    void listAsAnonymous() {
        StudyPostComment comment = Fixtures.comment(1L, post, writer, "댓글");
        given(studyPostRepository.existsById(10L)).willReturn(true);
        given(commentRepository.findByPostId(org.mockito.ArgumentMatchers.eq(10L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment), PageRequest.of(0, 20), 1));

        PageResponse<CommentResponse> response = service.getList(10L, null, 0, 20);

        assertThat(response.getContent().get(0).getIsAuthor()).isFalse();
    }

    @Test
    @DisplayName("없는 글의 댓글 목록은 빈 목록이 아니라 STUDY_POST_NOT_FOUND이다")
    void listOnMissingPost() {
        given(studyPostRepository.existsById(99L)).willReturn(false);

        assertErrorCode(() -> service.getList(99L, null, 0, 20), ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("잘못된 page, size는 INVALID_INPUT이다")
    void listWithInvalidPaging() {
        assertErrorCode(() -> service.getList(10L, null, -1, 20), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.getList(10L, null, 0, 0), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.getList(10L, null, 0, 101), ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("작성자는 댓글 내용을 수정할 수 있다")
    void update() {
        StudyPostComment comment = Fixtures.comment(5L, post, writer, "처음 댓글");
        given(commentRepository.findWithUserById(5L)).willReturn(Optional.of(comment));

        CommentIdResponse response = service.update(5L, 2L, request("고친 댓글"));

        assertThat(response.getCommentId()).isEqualTo(5L);
        assertThat(comment.getContent()).isEqualTo("고친 댓글");
    }

    @Test
    @DisplayName("작성자가 아니면 댓글을 수정할 수 없고 내용은 그대로다 (NOT_COMMENT_AUTHOR)")
    void updateByOtherUser() {
        StudyPostComment comment = Fixtures.comment(5L, post, writer, "처음 댓글");
        given(commentRepository.findWithUserById(5L)).willReturn(Optional.of(comment));

        assertErrorCode(() -> service.update(5L, 3L, request("해킹")), ErrorCode.NOT_COMMENT_AUTHOR);
        assertThat(comment.getContent()).isEqualTo("처음 댓글");
    }

    @Test
    @DisplayName("없는 댓글은 수정, 삭제 모두 COMMENT_NOT_FOUND이다")
    void commentNotFound() {
        given(commentRepository.findWithUserById(99L)).willReturn(Optional.empty());

        assertErrorCode(() -> service.update(99L, 2L, request("x")), ErrorCode.COMMENT_NOT_FOUND);
        assertErrorCode(() -> service.delete(99L, 2L), ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자는 댓글을 삭제할 수 있다")
    void delete() {
        StudyPostComment comment = Fixtures.comment(5L, post, writer, "댓글");
        given(commentRepository.findWithUserById(5L)).willReturn(Optional.of(comment));

        service.delete(5L, 2L);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("작성자가 아니면 댓글을 삭제할 수 없다")
    void deleteByOtherUser() {
        StudyPostComment comment = Fixtures.comment(5L, post, writer, "댓글");
        given(commentRepository.findWithUserById(5L)).willReturn(Optional.of(comment));

        assertErrorCode(() -> service.delete(5L, 3L), ErrorCode.NOT_COMMENT_AUTHOR);
        verify(commentRepository, never()).delete(any());
    }
}
