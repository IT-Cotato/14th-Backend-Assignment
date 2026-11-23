package com.cotato.backend14th.domain.studypost.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository.CommentCount;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyPostService {

    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional
    public StudyPostIdResponse create(Long userId, StudyPostCreateRequest request) {
        // 토큰은 유효하지만 사용자가 없는 경우도 인증 실패로 봐요
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPost post = StudyPost.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .build();

        return new StudyPostIdResponse(studyPostRepository.save(post).getId());
    }

    // 쿼리 3번(목록, 전체 개수, 댓글 수)으로 끝나요. 글마다 작성자나 댓글 수를 따로 조회하지 않아요.
    public PageResponse<StudyPostSummaryResponse> search(String keyword, StudyStatus status, Part part,
                                                         int page, int size) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<StudyPost> posts = studyPostRepository.search(normalizedKeyword, status, part,
                PageRequest.of(page, size));

        Map<Long, Long> commentCounts = countComments(posts.getContent());
        return PageResponse.of(posts,
                post -> StudyPostSummaryResponse.of(post, commentCounts.getOrDefault(post.getId(), 0L)));
    }

    public StudyPostDetailResponse getDetail(Long postId, Long loginUserId) {
        StudyPost post = studyPostRepository.findWithUserById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
        return StudyPostDetailResponse.of(post, loginUserId);
    }

    @Transactional
    public StudyPostIdResponse update(Long postId, Long userId, StudyPostUpdateRequest request) {
        StudyPost post = findOwnedPost(postId, userId);
        post.update(request.title(), request.content(), request.studyStatus());
        return new StudyPostIdResponse(post.getId());
    }

    // 달린 댓글을 먼저 지우고 글을 지워요.
    @Transactional
    public void delete(Long postId, Long userId) {
        StudyPost post = findOwnedPost(postId, userId);
        commentRepository.deleteAllByPostId(post.getId());
        studyPostRepository.delete(post);
    }

    private StudyPost findOwnedPost(Long postId, Long userId) {
        StudyPost post = studyPostRepository.findWithUserById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
        if (!post.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_POST_AUTHOR);
        }
        return post;
    }

    private Map<Long, Long> countComments(List<StudyPost> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(StudyPost::getId).toList();
        return commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(CommentCount::getPostId, CommentCount::getCount));
    }
}
