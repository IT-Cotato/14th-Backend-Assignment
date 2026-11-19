package com.cotato.backend14th.domain.studypost.service;

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

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository.CommentCount;
import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostSummaryResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.service.UserService;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class StudyPostServiceTest {

    @Mock
    private StudyPostRepository studyPostRepository;
    @Mock
    private StudyPostCommentRepository commentRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private StudyPostService studyPostService;

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_ID = 2L;

    private User user(Long id, Part part) {
        User user = User.builder()
                .email("u" + id + "@gmail.com").password("hashed").name("이름")
                .nickname("닉네임" + id).generation(14).part(part)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private StudyPost post(Long id, User author) {
        StudyPost post = new StudyPost(author, "알고리즘 스터디", "주 2회 백준");
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    @Test
    void 처음_작성한_글은_모집_중이다() {
        given(userService.getUser(AUTHOR_ID)).willReturn(user(AUTHOR_ID, Part.BE));
        given(studyPostRepository.save(any(StudyPost.class))).willAnswer(invocation -> {
            StudyPost saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            assertThat(saved.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
            return saved;
        });

        Long postId = studyPostService.create(AUTHOR_ID, new StudyPostCreateRequest(" 알고리즘 ", "내용")).postId();

        assertThat(postId).isEqualTo(10L);
    }

    @Test
    void 목록은_키워드를_LIKE_패턴으로_바꾸고_댓글_수를_채운다() {
        StudyPost first = post(10L, user(AUTHOR_ID, Part.BE));
        StudyPost second = post(11L, user(OTHER_ID, Part.FE));
        given(studyPostRepository.search("%알고%", StudyStatus.RECRUITING, Part.BE, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 10), 2));
        given(commentRepository.countByPostIds(List.of(10L, 11L))).willReturn(List.of(count(10L, 3L)));

        PageResponse<StudyPostSummaryResponse> page =
                studyPostService.search(" 알고 ", StudyStatus.RECRUITING, Part.BE, 0, 10);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(StudyPostSummaryResponse::commentCount).containsExactly(3L, 0L);
        assertThat(page.content().get(0).author().nickname()).isEqualTo("닉네임1");
    }

    @Test
    void 키워드의_퍼센트와_밑줄은_글자_그대로_찾는다() {
        assertThat(StudyPostService.toLikePattern("100%_!")).isEqualTo("%100!%!_!!%");
        assertThat(StudyPostService.toLikePattern("  ")).isNull();
        assertThat(StudyPostService.toLikePattern(null)).isNull();
    }

    @Test
    void 상세에서_작성자_본인이면_isAuthor가_true다() {
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post(10L, user(AUTHOR_ID, Part.BE))));

        StudyPostDetailResponse mine = studyPostService.getDetail(10L, AUTHOR_ID);
        StudyPostDetailResponse others = studyPostService.getDetail(10L, OTHER_ID);
        StudyPostDetailResponse anonymous = studyPostService.getDetail(10L, null);

        assertThat(mine.isAuthor()).isTrue();
        assertThat(others.isAuthor()).isFalse();
        assertThat(anonymous.isAuthor()).isFalse();
    }

    @Test
    void 없는_글을_조회하면_STUDY_POST_NOT_FOUND() {
        given(studyPostRepository.findWithUserById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyPostService.getDetail(99L, null))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    void 수정은_보낸_필드만_바꾸고_모집_상태도_바꿀_수_있다() {
        StudyPost post = post(10L, user(AUTHOR_ID, Part.BE));
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        studyPostService.update(10L, AUTHOR_ID, new StudyPostUpdateRequest(null, null, StudyStatus.CLOSED));

        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
        assertThat(post.getTitle()).isEqualTo("알고리즘 스터디");

        studyPostService.update(10L, AUTHOR_ID, new StudyPostUpdateRequest("새 제목", null, StudyStatus.RECRUITING));

        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getContent()).isEqualTo("주 2회 백준");
        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
    }

    @Test
    void 작성자가_아니면_수정할_수_없다() {
        StudyPost post = post(10L, user(AUTHOR_ID, Part.BE));
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> studyPostService.update(10L, OTHER_ID,
                new StudyPostUpdateRequest("남의 글", null, null)))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_POST_AUTHOR);
        assertThat(post.getTitle()).isEqualTo("알고리즘 스터디");
    }

    @Test
    void 삭제하면_댓글을_먼저_지우고_글을_지운다() {
        StudyPost post = post(10L, user(AUTHOR_ID, Part.BE));
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        studyPostService.delete(10L, AUTHOR_ID);

        verify(commentRepository).deleteAllByPostId(10L);
        verify(studyPostRepository).delete(post);
    }

    @Test
    void 작성자가_아니면_삭제할_수_없다() {
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post(10L, user(AUTHOR_ID, Part.BE))));

        assertThatThrownBy(() -> studyPostService.delete(10L, OTHER_ID))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_POST_AUTHOR);
        verify(commentRepository, never()).deleteAllByPostId(any());
        verify(studyPostRepository, never()).delete(any());
    }

    private CommentCount count(Long postId, Long commentCount) {
        return new CommentCount() {
            @Override
            public Long getPostId() {
                return postId;
            }

            @Override
            public Long getCommentCount() {
                return commentCount;
            }
        };
    }
}
