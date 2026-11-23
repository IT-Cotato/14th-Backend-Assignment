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
class StudyPostServiceTest {

    @Mock private StudyPostRepository studyPostRepository;
    @Mock private UserRepository userRepository;
    @Mock private CreateStudyPostRequest createRequest;
    @InjectMocks private StudyPostService studyPostService;

    @Test
    @DisplayName("모집글 작성 성공")
    void createSuccess() throws Exception {
        User user = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(createRequest.getTitle()).thenReturn("알고리즘");
        when(createRequest.getContent()).thenReturn("주 2회");
        when(studyPostRepository.save(any(StudyPost.class))).thenAnswer(invocation -> {
            StudyPost post = invocation.getArgument(0);
            setId(post, 10L);
            return post;
        });

        CreateStudyPostResponse response = studyPostService.create(1L, createRequest);

        assertThat(response.getPostId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("작성자가 아니면 수정 불가")
    void updateForbidden() throws Exception {
        User author = user(1L);
        StudyPost post = post(10L, author);
        when(studyPostRepository.findById(10L)).thenReturn(Optional.of(post));
        UpdateStudyPostRequest request = new UpdateStudyPostRequest();
        request.setTitle("수정");

        assertThatThrownBy(() -> studyPostService.update(2L, 10L, request))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);
    }

    @Test
    @DisplayName("없는 글이면 삭제 실패")
    void deleteNotFound() {
        when(studyPostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPostService.delete(1L, 99L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
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

    private StudyPost post(Long id, User user) throws Exception {
        StudyPost post = StudyPost.builder()
                .user(user)
                .title("제목")
                .content("내용")
                .build();
        setId(post, id);
        return post;
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}