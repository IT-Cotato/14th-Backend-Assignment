package com.cotato.backend14th.domain.studypost.service;

import static com.cotato.backend14th.support.TestFixtures.studyPost;
import static com.cotato.backend14th.support.TestFixtures.studyPostCreateRequest;
import static com.cotato.backend14th.support.TestFixtures.studyPostUpdateRequest;
import static com.cotato.backend14th.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.cotato.backend14th.domain.comment.repository.CommentRepository;
import com.cotato.backend14th.domain.studypost.dto.StudyPostIdResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostSummaryResponse;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
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
    private CommentRepository commentRepository;

    @InjectMocks
    private StudyPostService studyPostService;

    private static Page<StudyPost> emptyPage() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    }

    // ---------- 작성 ----------

    @Test
    @DisplayName("모집글을 작성하면 작성자와 연결하고 모집 중 상태로 저장한다")
    void create() {
        // given
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(studyPostRepository.save(any(StudyPost.class))).willAnswer(invocation -> {
            StudyPost saved = invocation.getArgument(0);
            setField(saved, "id", 10L);
            return saved;
        });

        // when
        StudyPostIdResponse response =
                studyPostService.create(1L, studyPostCreateRequest("스프링 스터디", "매주 토요일에 모여요."));

        // then
        assertThat(response.getPostId()).isEqualTo(10L);

        ArgumentCaptor<StudyPost> captor = ArgumentCaptor.forClass(StudyPost.class);
        then(studyPostRepository).should().save(captor.capture());
        StudyPost saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(author);
        assertThat(saved.getTitle()).isEqualTo("스프링 스터디");
        assertThat(saved.getContent()).isEqualTo("매주 토요일에 모여요.");
        assertThat(saved.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
    }

    // ---------- 목록 ----------

    @Test
    @DisplayName("목록 검색어는 앞뒤 공백을 지워서 조회한다")
    void getListTrimsKeyword() {
        // given
        given(studyPostRepository.search(any(), any(), any(), any())).willReturn(emptyPage());

        // when
        studyPostService.getList("  spring ", null, null, 0, 10);

        // then
        then(studyPostRepository).should()
                .search(eq("spring"), isNull(), isNull(), eq(PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("공백만 있는 검색어는 검색어가 없는 것(null)으로 조회한다")
    void getListWithBlankKeyword() {
        // given
        given(studyPostRepository.search(any(), any(), any(), any())).willReturn(emptyPage());

        // when
        studyPostService.getList("   ", StudyStatus.RECRUITING, null, 0, 10);

        // then
        then(studyPostRepository).should()
                .search(isNull(), eq(StudyStatus.RECRUITING), isNull(), eq(PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("목록의 글마다 댓글 수를 넣고, 댓글이 없는 글은 0으로 넣는다")
    void getListWithCommentCounts() {
        // given
        User author = user(1L);
        StudyPost withComments = studyPost(1L, author);
        StudyPost withoutComments = studyPost(2L, author);
        given(studyPostRepository.search(any(), any(), any(), any())).willReturn(
                new PageImpl<>(List.of(withComments, withoutComments), PageRequest.of(0, 10), 2));
        // 댓글이 있는 글만 결과에 나와요: [글 id, 댓글 수]
        List<Object[]> counts = List.<Object[]>of(new Object[] {1L, 3L});
        given(commentRepository.countByPostIds(List.of(1L, 2L))).willReturn(counts);

        // when
        PageResponse<StudyPostSummaryResponse> response = studyPostService.getList(null, null, null, 0, 10);

        // then
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent())
                .extracting(StudyPostSummaryResponse::getPostId, StudyPostSummaryResponse::getCommentCount)
                .containsExactly(tuple(1L, 3L), tuple(2L, 0L));
    }

    @Test
    @DisplayName("조회 결과가 비어 있으면 댓글 수 쿼리를 보내지 않는다")
    void getListWithEmptyResult() {
        // given
        given(studyPostRepository.search(any(), any(), any(), any())).willReturn(emptyPage());

        // when
        PageResponse<StudyPostSummaryResponse> response = studyPostService.getList(null, null, null, 0, 10);

        // then
        assertThat(response.getContent()).isEmpty();
        then(commentRepository).should(never()).countByPostIds(any());
    }

    @ParameterizedTest(name = "page={0}, size={1}")
    @CsvSource({"-1, 10", "0, 0", "0, 101"})
    @DisplayName("페이지가 음수이거나 크기가 1~100을 벗어나면 INVALID_INPUT 예외가 나고 조회하지 않는다")
    void getListWithInvalidPage(int page, int size) {
        // when & then
        assertThatThrownBy(() -> studyPostService.getList(null, null, null, page, size))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verifyNoInteractions(studyPostRepository, commentRepository);
    }

    // ---------- 상세 ----------

    @Test
    @DisplayName("상세의 isAuthor는 작성자면 true, 다른 사람이나 비로그인이면 false다")
    void getDetailIsAuthor() {
        // given
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(studyPost(10L, user(1L))));

        // when & then
        assertThat(studyPostService.getDetail(10L, 1L)).extracting("isAuthor").isEqualTo(true);
        assertThat(studyPostService.getDetail(10L, 2L)).extracting("isAuthor").isEqualTo(false);
        assertThat(studyPostService.getDetail(10L, null)).extracting("isAuthor").isEqualTo(false);
    }

    @Test
    @DisplayName("없는 모집글을 조회하면 STUDY_POST_NOT_FOUND 예외가 난다")
    void getDetailNotFound() {
        // given
        given(studyPostRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyPostService.getDetail(99L, 1L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_POST_NOT_FOUND);
    }

    // ---------- 수정 ----------

    @Test
    @DisplayName("작성자가 수정하면 보낸 필드만 바뀐다")
    void updateOnlyGivenFields() {
        // given
        StudyPost post = studyPost(10L, user(1L));
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        // when: 모집 상태만 보내요
        StudyPostIdResponse response =
                studyPostService.update(10L, 1L, studyPostUpdateRequest(null, null, StudyStatus.CLOSED));

        // then
        assertThat(response.getPostId()).isEqualTo(10L);
        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
        assertThat(post.getTitle()).isEqualTo("스터디 모집글 10");
        assertThat(post.getContent()).isEqualTo("스터디 모집글 내용 10");
    }

    @Test
    @DisplayName("작성자가 아닌 사람이 수정하면 NOT_POST_AUTHOR 예외가 나고 내용은 그대로다")
    void updateByOtherUser() {
        // given
        StudyPost post = studyPost(10L, user(1L));
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() ->
                studyPostService.update(10L, 2L, studyPostUpdateRequest("몰래 바꾼 제목", null, StudyStatus.CLOSED)))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);

        assertThat(post.getTitle()).isEqualTo("스터디 모집글 10");
        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
    }

    // ---------- 삭제 ----------

    @Test
    @DisplayName("작성자가 삭제하면 댓글을 먼저 지우고 모집글을 지운다")
    void delete() {
        // given
        StudyPost post = studyPost(10L, user(1L));
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(post));

        // when
        studyPostService.delete(10L, 1L);

        // then: 댓글이 남아 있으면 외래키 때문에 글을 지울 수 없어서 순서가 중요해요
        InOrder inOrder = inOrder(commentRepository, studyPostRepository);
        inOrder.verify(commentRepository).deleteAllByPostId(10L);
        inOrder.verify(studyPostRepository).delete(post);
    }

    @Test
    @DisplayName("작성자가 아닌 사람이 삭제하면 NOT_POST_AUTHOR 예외가 나고 아무것도 지우지 않는다")
    void deleteByOtherUser() {
        // given
        given(studyPostRepository.findById(10L)).willReturn(Optional.of(studyPost(10L, user(1L))));

        // when & then
        assertThatThrownBy(() -> studyPostService.delete(10L, 2L))
                .isInstanceOf(Backend14thException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);

        then(commentRepository).should(never()).deleteAllByPostId(anyLong());
        then(studyPostRepository).should(never()).delete(any());
    }
}
