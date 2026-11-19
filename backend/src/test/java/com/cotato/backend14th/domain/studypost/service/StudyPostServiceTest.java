package com.cotato.backend14th.domain.studypost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
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
import com.cotato.backend14th.global.common.dto.PageResponse;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class StudyPostServiceTest {

    @Mock StudyPostRepository studyPostRepository;
    @Mock StudyPostCommentRepository commentRepository;
    @Mock UserService userService;
    @InjectMocks StudyPostService studyPostService;

    private final User author = TestFixtures.user(1L, Part.BE);

    @Test
    @DisplayName("모집글을 만들면 상태는 RECRUITING이다")
    void create_startsRecruiting() {
        given(userService.getUser(1L)).willReturn(author);
        given(studyPostRepository.save(any(StudyPost.class))).willAnswer(inv -> inv.getArgument(0));

        studyPostService.create(1L, new StudyPostCreateRequest("제목", "내용"));

        ArgumentCaptor<StudyPost> captor = ArgumentCaptor.forClass(StudyPost.class);
        verify(studyPostRepository).save(captor.capture());
        assertThat(captor.getValue().getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
        assertThat(captor.getValue().getUser()).isEqualTo(author);
    }

    @Test
    @DisplayName("목록은 키워드를 다듬고 최신순으로 조회하며 댓글 수를 채운다")
    void search_trimsKeywordAndCountsComments() {
        StudyPost post = TestFixtures.post(10L, author);
        given(studyPostRepository.search(eq("알고"), eq(StudyStatus.RECRUITING), eq(Part.BE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1));
        given(commentRepository.countByPostIds(List.of(10L))).willReturn(List.<Object[]>of(new Object[]{10L, 3L}));

        PageResponse<StudyPostSummaryResponse> result =
                studyPostService.search("  알고  ", StudyStatus.RECRUITING, Part.BE, 0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).commentCount()).isEqualTo(3L);
        assertThat(result.content().get(0).author().part()).isEqualTo(Part.BE);
        assertThat(result.totalElements()).isEqualTo(1);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(studyPostRepository).search(eq("알고"), eq(StudyStatus.RECRUITING), eq(Part.BE), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("빈 키워드는 조건에서 뺀다 (null)")
    void search_blankKeywordIsNull() {
        given(studyPostRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        PageResponse<StudyPostSummaryResponse> result = studyPostService.search("   ", null, null, 0, 10);

        assertThat(result.content()).isEmpty();
        verify(commentRepository, never()).countByPostIds(any());
    }

    @Test
    @DisplayName("상세 조회: 작성자면 isAuthor true, 비로그인이면 false")
    void getDetail_isAuthor() {
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(TestFixtures.post(10L, author)));

        StudyPostDetailResponse mine = studyPostService.getDetail(10L, 1L);
        StudyPostDetailResponse anonymous = studyPostService.getDetail(10L, null);

        assertThat(mine.isAuthor()).isTrue();
        assertThat(anonymous.isAuthor()).isFalse();
    }

    @Test
    @DisplayName("없는 글을 조회하면 STUDY_POST_NOT_FOUND")
    void getDetail_notFound() {
        given(studyPostRepository.findWithUserById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyPostService.getDetail(999L, null))
                .extracting("errorCode").isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("수정은 보낸 필드만 바꾼다 (모집 상태 변경 포함)")
    void update_partial() {
        StudyPost post = TestFixtures.post(10L, author);
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post));

        studyPostService.update(10L, 1L, new StudyPostUpdateRequest(null, null, StudyStatus.CLOSED));

        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
        assertThat(post.getTitle()).isEqualTo("제목10");
    }

    @Test
    @DisplayName("작성자가 아니면 수정할 수 없다 (NOT_POST_AUTHOR)")
    void update_notAuthor() {
        StudyPost post = TestFixtures.post(10L, author);
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> studyPostService.update(10L, 2L, new StudyPostUpdateRequest("해킹", null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_POST_AUTHOR);
        assertThat(post.getTitle()).isEqualTo("제목10");
    }

    @Test
    @DisplayName("삭제하면 댓글을 먼저 지우고 글을 지운다")
    void delete_removesComments() {
        StudyPost post = TestFixtures.post(10L, author);
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post));

        studyPostService.delete(10L, 1L);

        verify(commentRepository).deleteAllByPostId(10L);
        verify(studyPostRepository).delete(post);
    }

    @Test
    @DisplayName("작성자가 아니면 삭제할 수 없다 (NOT_POST_AUTHOR)")
    void delete_notAuthor() {
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(TestFixtures.post(10L, author)));

        assertThatThrownBy(() -> studyPostService.delete(10L, 2L))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_POST_AUTHOR);
        verify(commentRepository, never()).deleteAllByPostId(anyLong());
    }
}
