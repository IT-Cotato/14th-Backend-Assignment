package com.cotato.backend14th.domain.studypost.service;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository.PostCommentCount;

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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyPostService {

    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;
    private final StudyPostCommentRepository commentRepository;

    // 모집글 작성
    @Transactional
    public StudyPostIdResponse create(Long userId, StudyPostCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPost savedPost = studyPostRepository.save(
                new StudyPost(user, request.title(), request.content())
        );

        return new StudyPostIdResponse(savedPost.getId());
    }

    // 모집글 목록 (검색·필터·페이징, 최신순)
    @Transactional(readOnly = true)
    public PageResponse<StudyPostSummaryResponse> getStudyPosts(
            String keyword,
            StudyStatus status,
            Part part,
            int page,
            int size
    ) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<StudyPost> posts = studyPostRepository
                .search(normalizedKeyword, status, part, PageRequest.of(page, size));

        // 이 페이지 글들의 댓글 수를 쿼리 한 번으로 가져와요. (글마다 세면 N+1이에요)
        Map<Long, Long> commentCounts = countComments(posts.getContent());

        return PageResponse.from(posts.map(post -> StudyPostSummaryResponse.of(
                post,
                commentCounts.getOrDefault(post.getId(), 0L)
        )));
    }

    private Map<Long, Long> countComments(List<StudyPost> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(StudyPost::getId).toList();

        return commentRepository.countByStudyPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        PostCommentCount::getPostId,
                        PostCommentCount::getCommentCount
                ));
    }

    // 모집글 상세 (viewerId가 null이면 비로그인 사용자예요)
    @Transactional(readOnly = true)
    public StudyPostDetailResponse getStudyPost(Long postId, Long viewerId) {
        StudyPost post = studyPostRepository.findWithUserById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        return StudyPostDetailResponse.of(post, post.isWrittenBy(viewerId));
    }

    // 모집글 수정 (작성자만, 전달한 필드만)
    @Transactional
    public StudyPostIdResponse update(Long postId, Long userId, StudyPostUpdateRequest request) {
        StudyPost post = getPostWrittenBy(postId, userId);

        post.update(request.title(), request.content(), request.studyStatus());

        return new StudyPostIdResponse(post.getId());
    }

    // 모집글 삭제 (작성자만)
    // 글을 지우기 전에 그 글의 댓글을 쿼리 한 번으로 먼저 지워요.
    @Transactional
    public void delete(Long postId, Long userId) {
        StudyPost post = getPostWrittenBy(postId, userId);

        commentRepository.deleteAllByStudyPostId(post.getId());
        studyPostRepository.delete(post);
    }

    // 글이 없으면 404, 작성자가 아니면 403이에요.
    private StudyPost getPostWrittenBy(Long postId, Long userId) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        if (!post.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_POST_AUTHOR);
        }
        return post;
    }
}
