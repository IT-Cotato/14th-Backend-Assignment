package com.cotato.backend14th.domain.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cotato.backend14th.domain.user.Part;
import com.cotato.backend14th.domain.user.User;
import com.cotato.backend14th.domain.user.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class StudyPostCommentServiceTest {

    @Mock private StudyPostCommentRepository commentRepository;
    @Mock private StudyPostRepository studyPostRepository;
    @Mock private UserRepository userRepository;
    @Mock private CreateCommentRequest request;
    @InjectMocks private StudyPostCommentService commentService;

    @Test
    @DisplayName("댓글 작성 성공")
    void createSuccess() throws Exception {
        User user = user(1L);
        StudyPost post = post(10L, user, StudyStatus.RECRUITING);
        when(studyPostRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(request.getContent()).thenReturn("참여하고 싶어요");
        when(commentRepository.save(any(StudyPostComment.class))).thenAnswer(invocation -> {
            StudyPostComment comment = invocation.getArgument(0);
            setId(comment, 5L);
            return comment;
        });

        CreateCommentResponse response = commentService.create(1L, 10L, request);

        assertThat(response.getCommentId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("마감된 글에는 댓글 작성 불가")
    void createOnClosedPost() throws Exception {
        User user = user(1L);
        StudyPost post = post(10L, user, StudyStatus.CLOSED);
        when(studyPostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.create(1L, 10L, request))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_CLOSED);
    }

    @Test
    @DisplayName("남의 댓글은 삭제 불가")
    void deleteForbidden() throws Exception {
        User author = user(1L);
        StudyPost post = post(10L, author, StudyStatus.RECRUITING);
        StudyPostComment comment = comment(5L, post, author);
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(2L, 5L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
    }

    private User user(Long id) throws Exception {
        User user = User.builder()
                .email("test@gmail.com")
                .password("encoded")
                .name("한다은")
                .nickname("다은")
                .generation(14)
                .part(Part.BE)
                .build();
        setId(user, id);
        return user;
    }

    private StudyPost post(Long id, User user, StudyStatus status) throws Exception {
        StudyPost post = StudyPost.builder()
                .user(user)
                .title("제목")
                .content("내용")
                .build();
        setId(post, id);
        post.update(null, null, status);
        return post;
    }

    private StudyPostComment comment(Long id, StudyPost post, User user) throws Exception {
        StudyPostComment comment = StudyPostComment.builder()
                .studyPost(post)
                .user(user)
                .content("댓글")
                .build();
        setId(comment, id);
        return comment;
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}