package com.cotato.backend14th.domain.study.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyPostService {

    private static final int MAX_PAGE_SIZE = 100;
    // LIKE 검색에서 %, _ 를 글자 그대로 찾기 위한 이스케이프 문자 (Repository의 escape '!'와 맞춰요)
    private static final char LIKE_ESCAPE = '!';

    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional
    public StudyPostIdResponse create(Long userId, StudyPostCreateRequest request) {
        // 토큰은 유효한데 사용자가 없는 경우도 인증 실패로 처리해요
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPost saved = studyPostRepository.save(
                StudyPost.create(user, request.getTitle(), request.getContent()));
        return new StudyPostIdResponse(saved.getId());
    }

    public PageResponse<StudyPostSummaryResponse> getList(
            String keyword, StudyStatus status, Part part, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }

        Page<StudyPost> posts = studyPostRepository.search(
                toLikeKeyword(keyword), status, part, PageRequest.of(page, size));

        Map<Long, Long> commentCounts = countComments(posts.getContent());
        return PageResponse.from(posts.map(post ->
                StudyPostSummaryResponse.from(post, commentCounts.getOrDefault(post.getId(), 0L))));
    }

    // currentUserId는 로그인하지 않았으면 null이에요
    public StudyPostDetailResponse getDetail(Long postId, Long currentUserId) {
        return StudyPostDetailResponse.of(findPost(postId), currentUserId);
    }

    @Transactional
    public StudyPostIdResponse update(Long postId, Long userId, StudyPostUpdateRequest request) {
        StudyPost post = findPost(postId);
        validateAuthor(post, userId);

        // 값을 보냈는데 공백뿐인 제목·내용은 허용하지 않아요
        if (isBlankValue(request.getTitle()) || isBlankValue(request.getContent())) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }

        post.update(request.getTitle(), request.getContent(), request.getStudyStatus());
        return new StudyPostIdResponse(post.getId());
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        StudyPost post = findPost(postId);
        validateAuthor(post, userId);

        // 댓글이 글을 참조(FK)하므로 댓글을 먼저 쿼리 한 번으로 지운 뒤 글을 삭제해요
        commentRepository.deleteByStudyPostId(postId);
        studyPostRepository.delete(post);
    }

    // 현재 페이지 글들의 댓글 수를 쿼리 한 번으로 세요. 댓글이 없는 글은 맵에 없으므로 0으로 처리해요.
    private Map<Long, Long> countComments(List<StudyPost> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(StudyPost::getId).toList();
        return commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(PostCommentCount::getPostId, PostCommentCount::getCommentCount));
    }

    private StudyPost findPost(Long postId) {
        return studyPostRepository.findWithUserById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
    }

    private void validateAuthor(StudyPost post, Long userId) {
        if (!post.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_POST_AUTHOR);
        }
    }

    private static boolean isBlankValue(String value) {
        return value != null && value.isBlank();
    }

    // 비어 있는 키워드는 "조건 없음"으로 보고, 나머지는 LIKE 특수문자를 이스케이프해요
    private static String toLikeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim()
                .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }
}
