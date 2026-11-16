package com.cotato.backend14th.domain.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.study.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.study.dto.StudyPostIdResponse;
import com.cotato.backend14th.domain.study.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.study.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class StudyPostServiceTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long POST_ID = 10L;

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private StudyPostCommentRepository studyPostCommentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyPostService studyPostService;

    @Nested
    @DisplayName("모집글 작성")
    class Create {

        @Test
        @DisplayName("회원이 있으면 모집 중 상태로 저장하고 글 id를 반환한다")
        void create_success() {
            // given
            User author = savedUser(AUTHOR_ID);
            given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(author));
            given(studyPostRepository.save(any(StudyPost.class))).willAnswer(invocation -> {
                StudyPost saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", POST_ID);
                return saved;
            });

            // when
            StudyPostIdResponse response = studyPostService.create(AUTHOR_ID,
                    createRequest("알고리즘 스터디 모집", "주 2회 백준 풀이"));

            // then
            ArgumentCaptor<StudyPost> captor = ArgumentCaptor.forClass(StudyPost.class);
            verify(studyPostRepository).save(captor.capture());
            StudyPost saved = captor.getValue();

            assertThat(saved.getTitle()).isEqualTo("알고리즘 스터디 모집");
            assertThat(saved.getContent()).isEqualTo("주 2회 백준 풀이");
            assertThat(saved.getUser()).isEqualTo(author);
            assertThat(saved.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
            assertThat(response.getPostId()).isEqualTo(POST_ID);
        }
    }

    @Nested
    @DisplayName("모집글 수정")
    class Update {

        @Test
        @DisplayName("작성자면 보낸 필드로 글을 수정하고 글 id를 반환한다")
        void update_byAuthor() {
            // given
            StudyPost post = savedPost(POST_ID, savedUser(AUTHOR_ID));
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));

            // when
            StudyPostIdResponse response = studyPostService.update(POST_ID, AUTHOR_ID,
                    updateRequest("수정된 제목", "수정된 내용", StudyStatus.CLOSED));

            // then
            assertThat(post.getTitle()).isEqualTo("수정된 제목");
            assertThat(post.getContent()).isEqualTo("수정된 내용");
            assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
            assertThat(response.getPostId()).isEqualTo(POST_ID);
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_POST_AUTHOR 예외가 발생하고 글은 바뀌지 않는다")
        void update_byOtherUser() {
            // given
            StudyPost post = savedPost(POST_ID, savedUser(AUTHOR_ID));
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> studyPostService.update(POST_ID, OTHER_USER_ID,
                    updateRequest("수정된 제목", "수정된 내용", StudyStatus.CLOSED)))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_POST_AUTHOR);

            assertThat(post.getTitle()).isEqualTo("원래 제목");
            assertThat(post.getContent()).isEqualTo("원래 내용");
            assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
        }
    }

    @Nested
    @DisplayName("모집글 삭제")
    class Delete {

        @Test
        @DisplayName("작성자면 달린 댓글을 먼저 지우고 글을 지운다")
        void delete_byAuthor() {
            // given
            StudyPost post = savedPost(POST_ID, savedUser(AUTHOR_ID));
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));

            // when
            studyPostService.delete(POST_ID, AUTHOR_ID);

            // then
            InOrder inOrder = inOrder(studyPostCommentRepository, studyPostRepository);
            inOrder.verify(studyPostCommentRepository).deleteAllByStudyPostId(POST_ID);
            inOrder.verify(studyPostRepository).deleteById(POST_ID);
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_POST_AUTHOR 예외가 발생하고 아무것도 지우지 않는다")
        void delete_byOtherUser() {
            // given
            StudyPost post = savedPost(POST_ID, savedUser(AUTHOR_ID));
            given(studyPostRepository.findById(POST_ID)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> studyPostService.delete(POST_ID, OTHER_USER_ID))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_POST_AUTHOR);

            verify(studyPostCommentRepository, never()).deleteAllByStudyPostId(anyLong());
            verify(studyPostRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("모집글 상세 조회")
    class GetDetail {

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND 예외가 발생한다")
        void getDetail_notFound() {
            // given
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> studyPostService.getDetail(POST_ID, AUTHOR_ID))
                    .isInstanceOf(Backend14thException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
        }
    }

    // 엔티티와 요청 DTO에 setter가 없어서 리플렉션으로 필드를 채워요
    private User savedUser(Long id) {
        User user = User.create("user" + id + "@gmail.com", "encoded-password", "홍길동", "길동이" + id, 14, Part.BE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private StudyPost savedPost(Long id, User author) {
        StudyPost post = StudyPost.create("원래 제목", "원래 내용", author);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private StudyPostCreateRequest createRequest(String title, String content) {
        StudyPostCreateRequest request = new StudyPostCreateRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

    private StudyPostUpdateRequest updateRequest(String title, String content, StudyStatus status) {
        StudyPostUpdateRequest request = new StudyPostUpdateRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "content", content);
        ReflectionTestUtils.setField(request, "studyStatus", status);
        return request;
    }
}
