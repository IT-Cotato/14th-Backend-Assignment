package com.cotato.backend14th.domain.studypost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostIdResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostSummaryResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
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
class StudyPostServiceTest {

    @Mock
    private StudyPostRepository studyPostRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyPostService studyPostService;

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long POST_ID = 10L;

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

    private static StudyPost post(Long id, User author) {
        StudyPost post = StudyPost.builder()
                .user(author)
                .title("알고리즘 스터디 모집")
                .content("주 2회 백준 풀이")
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private static void assertErrorCode(Throwable thrown, ErrorCode expected) {
        assertThat(thrown).isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(expected);
    }

    @Nested
    @DisplayName("작성")
    class Create {

        @Test
        @DisplayName("작성자를 지정하고 RECRUITING 상태로 저장한 뒤 postId를 돌려줘요")
        void success() {
            User author = user(AUTHOR_ID, "길동이", Part.BE);
            given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(author));
            given(studyPostRepository.save(any(StudyPost.class)))
                    .willAnswer(invocation -> {
                        StudyPost saved = invocation.getArgument(0);
                        ReflectionTestUtils.setField(saved, "id", POST_ID);
                        return saved;
                    });

            StudyPostIdResponse response = studyPostService.create(
                    AUTHOR_ID, new StudyPostCreateRequest("알고리즘 스터디 모집", "주 2회 백준 풀이"));

            assertThat(response.getPostId()).isEqualTo(POST_ID);
            ArgumentCaptor<StudyPost> captor = ArgumentCaptor.forClass(StudyPost.class);
            verify(studyPostRepository).save(captor.capture());
            StudyPost saved = captor.getValue();
            assertThat(saved.getUser()).isSameAs(author);
            assertThat(saved.getTitle()).isEqualTo("알고리즘 스터디 모집");
            assertThat(saved.getContent()).isEqualTo("주 2회 백준 풀이");
            assertThat(saved.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
        }

        @Test
        @DisplayName("토큰의 유저가 없으면 UNAUTHORIZED를 던지고 저장하지 않아요")
        void userNotFound() {
            given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.empty());

            Throwable thrown = catchThrowable(() -> studyPostService.create(
                    AUTHOR_ID, new StudyPostCreateRequest("제목", "내용")));

            assertErrorCode(thrown, ErrorCode.UNAUTHORIZED);
            verify(studyPostRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("목록")
    class GetList {

        @Test
        @DisplayName("검색어 앞뒤 공백을 지우고 필터와 페이지를 그대로 넘긴 뒤 페이지 형식으로 바꿔요")
        void searchAndConvert() {
            User author = user(AUTHOR_ID, "길동이", Part.BE);
            Pageable pageable = PageRequest.of(1, 2);
            given(studyPostRepository.search(eq("알고리즘"), eq(StudyStatus.RECRUITING), eq(Part.BE), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(post(POST_ID, author)), pageable, 3));

            PageResponse<StudyPostSummaryResponse> response =
                    studyPostService.getList("  알고리즘  ", StudyStatus.RECRUITING, Part.BE, 1, 2);

            assertThat(response.getPage()).isEqualTo(1);
            assertThat(response.getSize()).isEqualTo(2);
            assertThat(response.getTotalElements()).isEqualTo(3);
            assertThat(response.getTotalPages()).isEqualTo(2);
            assertThat(response.getContent()).hasSize(1);

            StudyPostSummaryResponse item = response.getContent().get(0);
            assertThat(item.getPostId()).isEqualTo(POST_ID);
            assertThat(item.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
            assertThat(item.getAuthor().getNickname()).isEqualTo("길동이");
            assertThat(item.getAuthor().getGeneration()).isEqualTo(14);
            assertThat(item.getAuthor().getPart()).isEqualTo(Part.BE);
            assertThat(item.getCommentCount()).isZero();
        }

        @Test
        @DisplayName("빈 검색어(공백만 있는 값 포함)는 null로 넘겨서 검색 조건을 끄어요")
        void blankKeywordBecomesNull() {
            given(studyPostRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            PageResponse<StudyPostSummaryResponse> response = studyPostService.getList("   ", null, null, 0, 10);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
            verify(studyPostRepository).search(isNull(), isNull(), isNull(), eq(PageRequest.of(0, 10)));
        }
    }

    @Nested
    @DisplayName("상세")
    class GetDetail {

        @Test
        @DisplayName("작성자 본인이 보면 isAuthor가 true예요")
        void author() {
            given(studyPostRepository.findWithUserById(POST_ID))
                    .willReturn(Optional.of(post(POST_ID, user(AUTHOR_ID, "길동이", Part.BE))));

            StudyPostDetailResponse response = studyPostService.getDetail(POST_ID, AUTHOR_ID);

            assertThat(response.getIsAuthor()).isTrue();
            assertThat(response.getPostId()).isEqualTo(POST_ID);
            assertThat(response.getContent()).isEqualTo("주 2회 백준 풀이");
            assertThat(response.getAuthor().getNickname()).isEqualTo("길동이");
        }

        @Test
        @DisplayName("다른 사람이 보면 isAuthor가 false예요")
        void otherUser() {
            given(studyPostRepository.findWithUserById(POST_ID))
                    .willReturn(Optional.of(post(POST_ID, user(AUTHOR_ID, "길동이", Part.BE))));

            assertThat(studyPostService.getDetail(POST_ID, OTHER_ID).getIsAuthor()).isFalse();
        }

        @Test
        @DisplayName("로그인하지 않았으면(null) isAuthor가 false예요")
        void anonymous() {
            given(studyPostRepository.findWithUserById(POST_ID))
                    .willReturn(Optional.of(post(POST_ID, user(AUTHOR_ID, "길동이", Part.BE))));

            assertThat(studyPostService.getDetail(POST_ID, null).getIsAuthor()).isFalse();
        }

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND를 던져요")
        void notFound() {
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studyPostService.getDetail(POST_ID, AUTHOR_ID))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.STUDY_POST_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("작성자가 보낸 필드만 바뀌고, 보내지 않은(null) 필드는 그대로예요")
        void onlySentFieldsChange() {
            StudyPost post = post(POST_ID, user(AUTHOR_ID, "길동이", Part.BE));
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

            StudyPostIdResponse response = studyPostService.update(
                    POST_ID, AUTHOR_ID, new StudyPostUpdateRequest(null, null, StudyStatus.CLOSED));

            assertThat(response.getPostId()).isEqualTo(POST_ID);
            assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
            assertThat(post.getTitle()).isEqualTo("알고리즘 스터디 모집");
            assertThat(post.getContent()).isEqualTo("주 2회 백준 풀이");
        }

        @Test
        @DisplayName("마감한 글을 다시 RECRUITING으로 바꾸고 제목·내용도 함께 바꿀 수 있어요")
        void reopenAndEdit() {
            StudyPost post = post(POST_ID, user(AUTHOR_ID, "길동이", Part.BE));
            post.update(null, null, StudyStatus.CLOSED);
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

            studyPostService.update(POST_ID, AUTHOR_ID,
                    new StudyPostUpdateRequest("수정된 제목", "수정된 내용", StudyStatus.RECRUITING));

            assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
            assertThat(post.getTitle()).isEqualTo("수정된 제목");
            assertThat(post.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_POST_AUTHOR를 던지고 글은 바뀌지 않아요")
        void notAuthor() {
            StudyPost post = post(POST_ID, user(AUTHOR_ID, "길동이", Part.BE));
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

            assertThatThrownBy(() -> studyPostService.update(
                    POST_ID, OTHER_ID, new StudyPostUpdateRequest("남이 바꾼 제목", null, StudyStatus.CLOSED)))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.NOT_POST_AUTHOR));
            assertThat(post.getTitle()).isEqualTo("알고리즘 스터디 모집");
            assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
        }

        @Test
        @DisplayName("없는 글이면 작성자 확인 전에 STUDY_POST_NOT_FOUND를 던져요")
        void notFound() {
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studyPostService.update(
                    POST_ID, OTHER_ID, new StudyPostUpdateRequest("제목", null, null)))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.STUDY_POST_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("작성자면 글을 삭제해요")
        void success() {
            StudyPost post = post(POST_ID, user(AUTHOR_ID, "길동이", Part.BE));
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

            studyPostService.delete(POST_ID, AUTHOR_ID);

            verify(studyPostRepository).delete(post);
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_POST_AUTHOR를 던지고 삭제하지 않아요")
        void notAuthor() {
            given(studyPostRepository.findWithUserById(POST_ID))
                    .willReturn(Optional.of(post(POST_ID, user(AUTHOR_ID, "길동이", Part.BE))));

            assertThatThrownBy(() -> studyPostService.delete(POST_ID, OTHER_ID))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.NOT_POST_AUTHOR));
            verify(studyPostRepository, never()).delete(any());
        }

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND를 던지고 삭제하지 않아요")
        void notFound() {
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studyPostService.delete(POST_ID, AUTHOR_ID))
                    .satisfies(thrown -> assertErrorCode(thrown, ErrorCode.STUDY_POST_NOT_FOUND));
            verify(studyPostRepository, never()).delete(any());
        }
    }
}
