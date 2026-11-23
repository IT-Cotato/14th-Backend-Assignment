package com.cotato.backend14th.domain.studypost;

import static com.cotato.backend14th.support.Fixtures.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
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
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository.CommentCountView;
import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostIdResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostListResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.studypost.service.StudyPostService;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.support.Fixtures;

@ExtendWith(MockitoExtension.class)
class StudyPostServiceTest {

    @Mock
    StudyPostRepository studyPostRepository;
    @Mock
    StudyPostCommentRepository commentRepository;
    @Mock
    UserRepository userRepository;
    @InjectMocks
    StudyPostService service;

    private final User author = Fixtures.user(1L, "author", Part.BE);
    private final User other = Fixtures.user(2L, "other", Part.FE);

    @Test
    @DisplayName("글을 쓰면 작성자와 함께 저장되고 처음 상태는 모집 중이다")
    void create() {
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(studyPostRepository.save(any(StudyPost.class))).willAnswer(invocation -> {
            StudyPost saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });
        StudyPostCreateRequest request =
                Fixtures.request(StudyPostCreateRequest.class, Map.of("title", "알고리즘 스터디", "content", "주 2회"));

        StudyPostIdResponse response = service.create(1L, request);

        assertThat(response.getPostId()).isEqualTo(10L);
        ArgumentCaptor<StudyPost> captor = ArgumentCaptor.forClass(StudyPost.class);
        verify(studyPostRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(author);
        assertThat(captor.getValue().getTitle()).isEqualTo("알고리즘 스터디");
        assertThat(captor.getValue().getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
    }

    @Test
    @DisplayName("토큰의 사용자가 DB에 없으면 UNAUTHORIZED이고 저장하지 않는다")
    void createWithUnknownUser() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());
        StudyPostCreateRequest request =
                Fixtures.request(StudyPostCreateRequest.class, Map.of("title", "제목", "content", "내용"));

        assertErrorCode(() -> service.create(99L, request), ErrorCode.UNAUTHORIZED);
        verify(studyPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("page가 음수이거나 size가 1~100을 벗어나면 INVALID_INPUT이다")
    void listWithInvalidPaging() {
        assertErrorCode(() -> service.getList(null, null, null, -1, 10), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.getList(null, null, null, 0, 0), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.getList(null, null, null, 0, 101), ErrorCode.INVALID_INPUT);
        verify(studyPostRepository, never()).search(any(), any(), any(), any());
    }

    @Test
    @DisplayName("공백뿐인 키워드는 검색 조건으로 쓰지 않는다")
    void listWithBlankKeyword() {
        given(studyPostRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PageResponse<StudyPostListResponse> response = service.getList("   ", null, null, 0, 10);

        assertThat(response.getContent()).isEmpty();
        // 글이 없으면 댓글 수를 세는 쿼리를 보내지 않는다
        verify(commentRepository, never()).countByPostIds(any());
    }

    @Test
    @DisplayName("목록은 글마다 댓글 수를 채우고, 댓글이 없는 글은 0이다")
    void listFillsCommentCount() {
        StudyPost withComments = Fixtures.post(1L, author, "댓글 있는 글");
        StudyPost withoutComments = Fixtures.post(2L, other, "댓글 없는 글");
        given(studyPostRepository.search(eqKeyword("스프링"), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(withComments, withoutComments), PageRequest.of(0, 10), 2));
        CommentCountView count = mock(CommentCountView.class);
        given(count.getPostId()).willReturn(1L);
        given(count.getCommentCount()).willReturn(3L);
        given(commentRepository.countByPostIds(List.of(1L, 2L))).willReturn(List.of(count));

        PageResponse<StudyPostListResponse> response = service.getList(" 스프링 ", null, null, 0, 10);

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).extracting(StudyPostListResponse::getCommentCount).containsExactly(3L, 0L);
        assertThat(response.getContent().get(0).getAuthor().getNickname()).isEqualTo("author");
    }

    private static String eqKeyword(String keyword) {
        return org.mockito.ArgumentMatchers.eq(keyword);
    }

    @Test
    @DisplayName("없는 글의 상세는 STUDY_POST_NOT_FOUND이다")
    void detailNotFound() {
        given(studyPostRepository.findWithUserById(99L)).willReturn(Optional.empty());

        assertErrorCode(() -> service.getDetail(99L, null), ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("상세의 isAuthor는 작성자 본인일 때만 true이고, 로그인하지 않으면 false이다")
    void detailIsAuthor() {
        StudyPost post = Fixtures.post(10L, author, "제목");
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post));

        StudyPostDetailResponse byAuthor = service.getDetail(10L, 1L);
        StudyPostDetailResponse byOther = service.getDetail(10L, 2L);
        StudyPostDetailResponse byAnonymous = service.getDetail(10L, null);

        assertThat(byAuthor.getIsAuthor()).isTrue();
        assertThat(byOther.getIsAuthor()).isFalse();
        assertThat(byAnonymous.getIsAuthor()).isFalse();
    }

    @Test
    @DisplayName("수정은 보낸 필드만 바꾼다. 상태만 보내면 제목은 그대로다")
    void updateOnlyGivenFields() {
        StudyPost post = Fixtures.post(10L, author, "원래 제목");
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post));
        StudyPostUpdateRequest request =
                Fixtures.request(StudyPostUpdateRequest.class, Map.of("studyStatus", StudyStatus.CLOSED));

        StudyPostIdResponse response = service.update(10L, 1L, request);

        assertThat(response.getPostId()).isEqualTo(10L);
        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
        assertThat(post.getTitle()).isEqualTo("원래 제목");
        assertThat(post.getContent()).isEqualTo("내용");
    }

    @Test
    @DisplayName("제목과 내용만 보내면 모집 상태는 그대로다")
    void updateTitleKeepsStatus() {
        StudyPost post = Fixtures.post(10L, author, "원래 제목");
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post));
        StudyPostUpdateRequest request =
                Fixtures.request(StudyPostUpdateRequest.class, Map.of("title", "새 제목", "content", "새 내용"));

        service.update(10L, 1L, request);

        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getContent()).isEqualTo("새 내용");
        assertThat(post.getStudyStatus()).isEqualTo(StudyStatus.RECRUITING);
    }

    @Test
    @DisplayName("작성자가 아니면 수정할 수 없고 글은 그대로다 (NOT_POST_AUTHOR)")
    void updateByOtherUser() {
        StudyPost post = Fixtures.post(10L, author, "원래 제목");
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post));
        StudyPostUpdateRequest request = Fixtures.request(StudyPostUpdateRequest.class, Map.of("title", "해킹"));

        assertErrorCode(() -> service.update(10L, 2L, request), ErrorCode.NOT_POST_AUTHOR);
        assertThat(post.getTitle()).isEqualTo("원래 제목");
    }

    @Test
    @DisplayName("없는 글을 수정하면 STUDY_POST_NOT_FOUND이다")
    void updateNotFound() {
        given(studyPostRepository.findWithUserById(99L)).willReturn(Optional.empty());
        StudyPostUpdateRequest request = Fixtures.request(StudyPostUpdateRequest.class, Map.of("title", "x"));

        assertErrorCode(() -> service.update(99L, 1L, request), ErrorCode.STUDY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("글을 삭제하면 댓글을 먼저 지우고 글을 지운다")
    void deleteRemovesCommentsFirst() {
        StudyPost post = Fixtures.post(10L, author, "제목");
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post));

        service.delete(10L, 1L);

        InOrder order = inOrder(commentRepository, studyPostRepository);
        order.verify(commentRepository).deleteByStudyPostId(10L);
        order.verify(studyPostRepository).delete(post);
    }

    @Test
    @DisplayName("작성자가 아니면 삭제할 수 없고 아무것도 지워지지 않는다")
    void deleteByOtherUser() {
        StudyPost post = Fixtures.post(10L, author, "제목");
        given(studyPostRepository.findWithUserById(10L)).willReturn(Optional.of(post));

        assertErrorCode(() -> service.delete(10L, 2L), ErrorCode.NOT_POST_AUTHOR);
        verify(commentRepository, never()).deleteByStudyPostId(any());
        verify(studyPostRepository, never()).delete(any());
    }
}
