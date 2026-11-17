package com.cotato.backend14th.domain.studypost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.cotato.backend14th.domain.studypost.dto.request.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.request.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.dto.response.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.response.StudyPostIdResponse;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class StudyPostServiceTest {

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyPostCommentRepository studyPostCommentRepository;

    @Mock
    private StudyPostCreateRequest createRequest;

    @Mock
    private StudyPostUpdateRequest updateRequest;

    @InjectMocks
    private StudyPostService studyPostService;

    private User author(Long id) {
        return new User(id, "author@example.com", "encoded", "홍길동", "길동이", 14, Part.BE);
    }

    @Test
    void create_success() {
        User user = author(1L);
        when(createRequest.getTitle()).thenReturn("알고리즘 스터디");
        when(createRequest.getContent()).thenReturn("내용");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(studyPostRepository.save(any(StudyPost.class)))
                .thenAnswer(invocation -> {
                    StudyPost arg = invocation.getArgument(0);
                    assertThat(arg.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
                    return new StudyPost(10L, user, arg.getTitle(), arg.getContent(), StudyStatus.RECRUITING);
                });

        StudyPostIdResponse response = studyPostService.create(1L, createRequest);

        assertThat(response.getPostId()).isEqualTo(10L);
    }

    @Test
    void search_withKeywordOnly() {
        User user = author(1L);
        StudyPost post = new StudyPost(10L, user, "알고리즘 스터디", "내용", StudyStatus.RECRUITING);
        Page<StudyPost> page = new PageImpl<>(java.util.List.of(post), PageRequest.of(0, 10), 1);
        when(studyPostRepository.search(eq("알고리즘"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<?> result = studyPostService.search("알고리즘", null, null, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void search_withStatusAndPartFilter() {
        Page<StudyPost> emptyPage = new PageImpl<>(java.util.List.of(), PageRequest.of(0, 10), 0);
        when(studyPostRepository.search(isNull(), eq(StudyStatus.CLOSED), eq(Part.BE), any(Pageable.class)))
                .thenReturn(emptyPage);

        studyPostService.search(null, "CLOSED", "BE", 0, 10);

        verify(studyPostRepository).search(isNull(), eq(StudyStatus.CLOSED), eq(Part.BE), any(Pageable.class));
    }

    @Test
    void search_invalidStatus_throwsInvalidInput() {
        assertThatThrownBy(() -> studyPostService.search(null, "XX", null, 0, 10))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void search_invalidPart_throwsInvalidInput() {
        assertThatThrownBy(() -> studyPostService.search(null, null, "ZZ", 0, 10))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void search_negativePage_throwsInvalidInput() {
        assertThatThrownBy(() -> studyPostService.search(null, null, null, -1, 10))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void search_zeroSize_throwsInvalidInput() {
        assertThatThrownBy(() -> studyPostService.search(null, null, null, 0, 0))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void getDetail_found_isAuthorTrue() {
        User user = author(1L);
        StudyPost post = new StudyPost(10L, user, "제목", "내용", StudyStatus.RECRUITING);
        when(studyPostRepository.findByIdWithUser(10L)).thenReturn(Optional.of(post));

        StudyPostDetailResponse response = studyPostService.getDetail(10L, 1L);

        assertThat(response.getIsAuthor()).isTrue();
    }

    @Test
    void getDetail_found_isAuthorFalseWhenUserIdNull() {
        User user = author(1L);
        StudyPost post = new StudyPost(10L, user, "제목", "내용", StudyStatus.RECRUITING);
        when(studyPostRepository.findByIdWithUser(10L)).thenReturn(Optional.of(post));

        StudyPostDetailResponse response = studyPostService.getDetail(10L, null);

        assertThat(response.getIsAuthor()).isFalse();
    }

    @Test
    void getDetail_found_isAuthorFalseForOtherUser() {
        User user = author(1L);
        StudyPost post = new StudyPost(10L, user, "제목", "내용", StudyStatus.RECRUITING);
        when(studyPostRepository.findByIdWithUser(10L)).thenReturn(Optional.of(post));

        StudyPostDetailResponse response = studyPostService.getDetail(10L, 2L);

        assertThat(response.getIsAuthor()).isFalse();
    }

    @Test
    void getDetail_notFound_throwsStudyPostNotFound() {
        when(studyPostRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPostService.getDetail(999L, 1L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    void update_success_partialFields() {
        User user = author(1L);
        StudyPost post = new StudyPost(10L, user, "원래 제목", "원래 내용", StudyStatus.RECRUITING);
        when(studyPostRepository.findByIdWithUser(10L)).thenReturn(Optional.of(post));
        when(updateRequest.getTitle()).thenReturn("새 제목");
        when(updateRequest.getContent()).thenReturn(null);
        when(updateRequest.getStudyStatus()).thenReturn(null);

        StudyPostIdResponse response = studyPostService.update(10L, 1L, updateRequest);

        assertThat(response.getPostId()).isEqualTo(10L);
        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getContent()).isEqualTo("원래 내용");
    }

    @Test
    void update_notFound_throwsStudyPostNotFound() {
        when(studyPostRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPostService.update(999L, 1L, updateRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    void update_notAuthor_throwsNotPostAuthor() {
        User user = author(1L);
        StudyPost post = new StudyPost(10L, user, "제목", "내용", StudyStatus.RECRUITING);
        when(studyPostRepository.findByIdWithUser(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> studyPostService.update(10L, 2L, updateRequest))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);
    }

    @Test
    void delete_success() {
        User user = author(1L);
        StudyPost post = new StudyPost(10L, user, "제목", "내용", StudyStatus.RECRUITING);
        when(studyPostRepository.findByIdWithUser(10L)).thenReturn(Optional.of(post));

        studyPostService.delete(10L, 1L);

        verify(studyPostCommentRepository).deleteByStudyPostId(10L);
        verify(studyPostRepository).delete(post);
    }

    @Test
    void delete_notFound_throwsStudyPostNotFound() {
        when(studyPostRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPostService.delete(999L, 1L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
        verify(studyPostRepository, never()).delete(any());
    }

    @Test
    void delete_notAuthor_throwsNotPostAuthor() {
        User user = author(1L);
        StudyPost post = new StudyPost(10L, user, "제목", "내용", StudyStatus.RECRUITING);
        when(studyPostRepository.findByIdWithUser(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> studyPostService.delete(10L, 2L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);
        verify(studyPostRepository, never()).delete(any());
    }
}
