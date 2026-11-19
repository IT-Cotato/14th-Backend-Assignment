package com.cotato.backend14th.domain.studypost.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
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
import com.cotato.backend14th.domain.user.service.UserService;
import com.cotato.backend14th.global.common.dto.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyPostService {

    private static final Sort LATEST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository commentRepository;
    private final UserService userService;

    @Transactional
    public StudyPostIdResponse create(Long userId, StudyPostCreateRequest request) {
        User user = userService.getUser(userId);
        StudyPost post = StudyPost.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .build();
        return new StudyPostIdResponse(studyPostRepository.save(post).getId());
    }

    public PageResponse<StudyPostSummaryResponse> search(String keyword, StudyStatus status, Part part,
                                                         int page, int size) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<StudyPost> posts = studyPostRepository.search(normalizedKeyword, status, part,
                PageRequest.of(Math.max(page, 0), clampSize(size), LATEST));

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
        StudyPost post = getOwnedPost(postId, userId);
        post.update(request.title(), request.content(), request.studyStatus());
        return new StudyPostIdResponse(post.getId());
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        StudyPost post = getOwnedPost(postId, userId);
        commentRepository.deleteAllByPostId(post.getId()); // 댓글 먼저 지우고
        studyPostRepository.delete(post);                  // 글을 지워요
    }

    public StudyPost getPost(Long postId) {
        return studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
    }

    private StudyPost getOwnedPost(Long postId, Long userId) {
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
        List<Long> ids = posts.stream().map(StudyPost::getId).toList();
        return commentRepository.countByPostIds(ids).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
