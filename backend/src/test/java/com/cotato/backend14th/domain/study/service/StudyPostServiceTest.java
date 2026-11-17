package com.cotato.backend14th.domain.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository.PostCommentCount;
import com.cotato.backend14th.domain.study.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.study.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.study.dto.StudyPostIdResponse;
import com.cotato.backend14th.domain.study.dto.StudyPostSummaryResponse;
import com.cotato.backend14th.domain.study.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.study.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
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
    private StudyPostCommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyPostService studyPostService;

    private static PostCommentCount commentCount(Long postId, long count) {
        return new PostCommentCount() {
            @Override
            public Long getPostId() {
                return postId;
            }

            @Override
            public long getCommentCount() {
                return count;
            }
        };
    }

    private static void assertErrorCode(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(Backend14thException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private static User userWithId(Long id) {
        User user = User.create("user" + id + "@example.com", "encoded", "tester", "nick" + id, 14, Part.BE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static StudyPost postBy(User author) {
        StudyPost post = StudyPost.create(author, "원래 제목", "원래 내용");
        ReflectionTestUtils.setField(post, "id", POST_ID);
        return post;
    }

    @Nested
    @DisplayName("모집글 작성")
    class Create {

        @Test
        @DisplayName("모집 중(RECRUITING) 상태로 저장하고 글 ID를 반환한다")
        void savesAsRecruiting() {
            User author = userWithId(AUTHOR_ID);
            given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(author));
            given(studyPostRepository.save(any(StudyPost.class))).willAnswer(invocation -> {
                StudyPost saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", POST_ID);
                return saved;
            });

            StudyPostIdResponse response =
                    studyPostService.create(AUTHOR_ID, new StudyPostCreateRequest("제목", "내용"));

            assertThat(response.getPostId()).isEqualTo(POST_ID);
            ArgumentCaptor<StudyPost> captor = ArgumentCaptor.forClass(StudyPost.class);
            verify(studyPostRepository).save(captor.capture());
            assertThat(captor.getValue().getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
            assertThat(captor.getValue().getUser()).isSameAs(author);
            assertThat(captor.getValue().getTitle()).isEqualTo("제목");
        }

        @Test
        @DisplayName("토큰의 사용자가 존재하지 않으면 UNAUTHORIZED이고 저장하지 않는다")
        void rejectsUnknownUser() {
            given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.empty());

            assertErrorCode(() -> studyPostService.create(AUTHOR_ID, new StudyPostCreateRequest("제목", "내용")),
                    ErrorCode.UNAUTHORIZED);

            verify(studyPostRepository, never()).save(any(StudyPost.class));
        }
    }

    @Nested
    @DisplayName("모집글 목록")
    class GetList {

        @Test
        @DisplayName("필터를 그대로 전달하고, 키워드의 LIKE 특수문자는 이스케이프한다")
        void passesFiltersAndEscapesKeyword() {
            StudyPost post = postBy(userWithId(AUTHOR_ID));
            // 공백은 제거하고, '!' → '!!', '%' → '!%', '_' → '!_' 로 바뀐다
            given(studyPostRepository.search(eq("100!%!_a!!b"), eq(StudyStatus.RECRUITING), eq(Part.BE), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(post), PageRequest.of(1, 5), 6));
            given(commentRepository.countByPostIds(List.of(POST_ID))).willReturn(List.of(commentCount(POST_ID, 3L)));

            PageResponse<StudyPostSummaryResponse> response = studyPostService.getList(
                    "  100%_a!b  ", StudyStatus.RECRUITING, Part.BE, 1, 5);

            assertThat(response.getPage()).isEqualTo(1);
            assertThat(response.getSize()).isEqualTo(5);
            assertThat(response.getTotalElements()).isEqualTo(6);
            assertThat(response.getTotalPages()).isEqualTo(2);
            StudyPostSummaryResponse item = response.getContent().get(0);
            assertThat(item.getPostId()).isEqualTo(POST_ID);
            assertThat(item.getAuthor().getNickname()).isEqualTo("nick1");
            assertThat(item.getAuthor().getPart()).isEqualTo(Part.BE);
            assertThat(item.getCommentCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("댓글이 없는 글의 댓글 수는 0이고, 글 여러 개의 댓글 수를 쿼리 한 번으로 센다")
        void countsCommentsInOneQueryAndDefaultsToZero() {
            StudyPost withComments = postBy(userWithId(AUTHOR_ID));
            StudyPost withoutComments = postBy(userWithId(AUTHOR_ID));
            ReflectionTestUtils.setField(withoutComments, "id", POST_ID + 1);
            given(studyPostRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(withComments, withoutComments)));
            // 댓글이 없는 글(POST_ID + 1)은 집계 결과에 나타나지 않는다
            given(commentRepository.countByPostIds(List.of(POST_ID, POST_ID + 1)))
                    .willReturn(List.of(commentCount(POST_ID, 2L)));

            PageResponse<StudyPostSummaryResponse> response = studyPostService.getList(null, null, null, 0, 10);

            assertThat(response.getContent()).extracting(StudyPostSummaryResponse::getCommentCount)
                    .containsExactly(2L, 0L);
            verify(commentRepository).countByPostIds(List.of(POST_ID, POST_ID + 1));
        }

        @Test
        @DisplayName("키워드가 비어 있거나 공백뿐이면 조건 없음(null)으로 조회한다")
        void treatsBlankKeywordAsNoFilter() {
            given(studyPostRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of()));

            studyPostService.getList("   ", null, null, 0, 10);
            studyPostService.getList(null, null, null, 0, 10);

            verify(studyPostRepository, org.mockito.Mockito.times(2))
                    .search(isNull(), isNull(), isNull(), any(Pageable.class));
            // 결과가 없으면 댓글 수를 조회하지 않는다
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("page가 음수이거나 size가 1~100을 벗어나면 INVALID_INPUT이다")
        void rejectsInvalidPaging() {
            assertErrorCode(() -> studyPostService.getList(null, null, null, -1, 10), ErrorCode.INVALID_INPUT);
            assertErrorCode(() -> studyPostService.getList(null, null, null, 0, 0), ErrorCode.INVALID_INPUT);
            assertErrorCode(() -> studyPostService.getList(null, null, null, 0, 101), ErrorCode.INVALID_INPUT);

            verifyNoInteractions(studyPostRepository);
        }
    }

    @Nested
    @DisplayName("모집글 상세")
    class GetDetail {

        @Test
        @DisplayName("작성자 본인이 조회하면 isAuthor가 true이다")
        void authorSeesIsAuthorTrue() {
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(postBy(userWithId(AUTHOR_ID))));

            StudyPostDetailResponse response = studyPostService.getDetail(POST_ID, AUTHOR_ID);

            assertThat(response.isMine()).isTrue();
            assertThat(response.getContent()).isEqualTo("원래 내용");
        }

        @Test
        @DisplayName("다른 사용자나 로그인하지 않은 사용자(null)가 조회하면 isAuthor가 false이다")
        void othersSeeIsAuthorFalse() {
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(postBy(userWithId(AUTHOR_ID))));

            assertThat(studyPostService.getDetail(POST_ID, OTHER_USER_ID).isMine()).isFalse();
            assertThat(studyPostService.getDetail(POST_ID, null).isMine()).isFalse();
        }

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND이다")
        void notFound() {
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

            assertErrorCode(() -> studyPostService.getDetail(POST_ID, null), ErrorCode.STUDY_POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("모집글 수정")
    class Update {

        private StudyPost post;

        private void givenPostWrittenByAuthor() {
            post = postBy(userWithId(AUTHOR_ID));
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));
        }

        @Test
        @DisplayName("보낸 필드만 바꾸고 나머지는 그대로 둔다")
        void updatesOnlyGivenFields() {
            givenPostWrittenByAuthor();

            studyPostService.update(POST_ID, AUTHOR_ID, new StudyPostUpdateRequest("새 제목", null, null));

            assertThat(post.getTitle()).isEqualTo("새 제목");
            assertThat(post.getContent()).isEqualTo("원래 내용");
            assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
        }

        @Test
        @DisplayName("모집 상태를 마감했다가 다시 모집 중으로 바꿀 수 있다")
        void togglesStatusFreely() {
            givenPostWrittenByAuthor();

            studyPostService.update(POST_ID, AUTHOR_ID, new StudyPostUpdateRequest(null, null, StudyStatus.CLOSED));
            assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);

            studyPostService.update(POST_ID, AUTHOR_ID, new StudyPostUpdateRequest(null, null, StudyStatus.RECRUITING));
            assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
        }

        @Test
        @DisplayName("아무 필드도 보내지 않으면 아무것도 바뀌지 않는다")
        void emptyRequestChangesNothing() {
            givenPostWrittenByAuthor();

            StudyPostIdResponse response =
                    studyPostService.update(POST_ID, AUTHOR_ID, new StudyPostUpdateRequest(null, null, null));

            assertThat(response.getPostId()).isEqualTo(POST_ID);
            assertThat(post.getTitle()).isEqualTo("원래 제목");
            assertThat(post.getContent()).isEqualTo("원래 내용");
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_POST_AUTHOR이고 내용은 바뀌지 않는다")
        void rejectsNonAuthor() {
            givenPostWrittenByAuthor();

            assertErrorCode(
                    () -> studyPostService.update(POST_ID, OTHER_USER_ID, new StudyPostUpdateRequest("해킹", null, null)),
                    ErrorCode.NOT_POST_AUTHOR);

            assertThat(post.getTitle()).isEqualTo("원래 제목");
        }

        @Test
        @DisplayName("제목이나 내용을 공백뿐인 값으로 보내면 INVALID_INPUT이고 바뀌지 않는다")
        void rejectsBlankValues() {
            givenPostWrittenByAuthor();

            assertErrorCode(() -> studyPostService.update(POST_ID, AUTHOR_ID, new StudyPostUpdateRequest("   ", null, null)),
                    ErrorCode.INVALID_INPUT);
            assertErrorCode(() -> studyPostService.update(POST_ID, AUTHOR_ID, new StudyPostUpdateRequest(null, " ", null)),
                    ErrorCode.INVALID_INPUT);

            assertThat(post.getTitle()).isEqualTo("원래 제목");
            assertThat(post.getContent()).isEqualTo("원래 내용");
        }

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND이다")
        void notFound() {
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

            assertErrorCode(
                    () -> studyPostService.update(POST_ID, AUTHOR_ID, new StudyPostUpdateRequest("제목", null, null)),
                    ErrorCode.STUDY_POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("모집글 삭제")
    class Delete {

        @Test
        @DisplayName("작성자는 삭제할 수 있고, 댓글을 먼저 지운 뒤 글을 삭제한다")
        void authorCanDeleteAndCommentsAreDeletedFirst() {
            StudyPost post = postBy(userWithId(AUTHOR_ID));
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(post));

            studyPostService.delete(POST_ID, AUTHOR_ID);

            // 댓글이 글을 참조(FK)하므로 순서가 바뀌면 삭제가 실패한다
            InOrder order = inOrder(commentRepository, studyPostRepository);
            order.verify(commentRepository).deleteByStudyPostId(POST_ID);
            order.verify(studyPostRepository).delete(post);
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_POST_AUTHOR이고 삭제하지 않는다")
        void rejectsNonAuthor() {
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.of(postBy(userWithId(AUTHOR_ID))));

            assertErrorCode(() -> studyPostService.delete(POST_ID, OTHER_USER_ID), ErrorCode.NOT_POST_AUTHOR);

            verify(studyPostRepository, never()).delete(any(StudyPost.class));
            // 권한이 없으면 댓글도 지우지 않는다
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("없는 글이면 STUDY_POST_NOT_FOUND이다")
        void notFound() {
            given(studyPostRepository.findWithUserById(POST_ID)).willReturn(Optional.empty());

            assertErrorCode(() -> studyPostService.delete(POST_ID, AUTHOR_ID), ErrorCode.STUDY_POST_NOT_FOUND);

            verify(studyPostRepository, never()).delete(any(StudyPost.class));
        }
    }
}
