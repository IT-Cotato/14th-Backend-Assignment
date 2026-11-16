package com.cotato.backend14th.domain.study.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyPostService {

    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository studyPostCommentRepository;
    private final UserRepository userRepository;

    // 토큰은 유효한데 회원이 없으면(탈퇴 등) 인증 실패로 봐요 (UserService와 같은 기준)
    @Transactional
    public StudyPostIdResponse create(Long userId, StudyPostCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPost post = StudyPost.create(request.getTitle(), request.getContent(), user);
        return new StudyPostIdResponse(studyPostRepository.save(post).getId());
    }

    // 쿼리는 목록(작성자 fetch join), 전체 개수, 댓글 수 이렇게 3번만 나가요
    public PageResponse<StudyPostSummaryResponse> getList(String keyword, StudyStatus status, Part part,
                                                         int page, int size) {
        String searchKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<StudyPost> posts = studyPostRepository.search(searchKeyword, status, part, PageRequest.of(page, size));

        Map<Long, Long> commentCounts = countComments(posts.getContent());
        List<StudyPostSummaryResponse> content = posts.getContent().stream()
                .map(post -> StudyPostSummaryResponse.of(post, commentCounts.getOrDefault(post.getId(), 0L)))
                .toList();

        return PageResponse.of(posts, content);
    }

    public StudyPostDetailResponse getDetail(Long postId, Long userId) {
        StudyPost post = studyPostRepository.findWithUserById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        // 비로그인이면 userId가 null이라 false예요
        return StudyPostDetailResponse.of(post, post.isAuthor(userId));
    }

    // 확인 순서: 글 조회(404) → 작성자 확인(403)
    @Transactional
    public StudyPostIdResponse update(Long postId, Long userId, StudyPostUpdateRequest request) {
        StudyPost post = getAuthorizedPost(postId, userId);
        post.update(request.getTitle(), request.getContent(), request.getStudyStatus());
        return new StudyPostIdResponse(post.getId());
    }

    // 달린 댓글을 먼저 지우고 글을 지워요 (DB cascade가 아닌 코드에서 삭제)
    @Transactional
    public void delete(Long postId, Long userId) {
        StudyPost post = getAuthorizedPost(postId, userId);
        studyPostCommentRepository.deleteAllByStudyPostId(post.getId());
        studyPostRepository.deleteById(post.getId());
    }

    private StudyPost getAuthorizedPost(Long postId, Long userId) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        if (!post.isAuthor(userId)) {
            throw new Backend14thException(ErrorCode.NOT_POST_AUTHOR);
        }
        return post;
    }

    // 이번 페이지 글들의 댓글 수를 한 번의 쿼리로 가져와요
    private Map<Long, Long> countComments(List<StudyPost> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(StudyPost::getId).toList();
        return studyPostCommentRepository.countGroupByPostIds(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
