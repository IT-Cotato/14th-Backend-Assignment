package com.cotato.backend14th.domain.studypost.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.comment.repository.CommentRepository;
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

    // 명세에 최대 크기가 없어서, 한 번에 너무 많이 가져오지 않도록 100으로 제한해요
    private static final int MAX_PAGE_SIZE = 100;

    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public StudyPostIdResponse create(Long userId, StudyPostCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPost post = StudyPost.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return new StudyPostIdResponse(studyPostRepository.save(post).getId());
    }

    public PageResponse<StudyPostSummaryResponse> getList(
            String keyword, StudyStatus status, Part part, int page, int size
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }

        // 빈 문자열이나 공백만 보내면 검색어가 없는 것으로 봐요
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<StudyPost> posts = studyPostRepository.search(
                normalizedKeyword, status, part, PageRequest.of(page, size));

        Map<Long, Long> commentCounts = countComments(posts.getContent());
        return PageResponse.from(posts.map(post ->
                StudyPostSummaryResponse.of(post, commentCounts.getOrDefault(post.getId(), 0L))));
    }

    // 현재 페이지에 있는 글들의 댓글 수를 쿼리 한 번으로 세요.
    // 글마다 따로 세면 글 10개에 쿼리가 10번 더 나가요.
    private Map<Long, Long> countComments(List<StudyPost> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(StudyPost::getId).toList();
        return commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    // userId는 로그인하지 않았으면 null이에요
    public StudyPostDetailResponse getDetail(Long postId, Long userId) {
        StudyPost post = findPost(postId);
        return StudyPostDetailResponse.of(post, post.isWrittenBy(userId));
    }

    @Transactional
    public StudyPostIdResponse update(Long postId, Long userId, StudyPostUpdateRequest request) {
        StudyPost post = findPost(postId);
        validateAuthor(post, userId);

        post.update(request.getTitle(), request.getContent(), request.getStudyStatus());
        return new StudyPostIdResponse(post.getId());
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        StudyPost post = findPost(postId);
        validateAuthor(post, userId);

        // 댓글을 먼저 지워요. 댓글이 남아 있으면 외래키 때문에 글을 지울 수 없어요.
        commentRepository.deleteAllByPostId(post.getId());
        studyPostRepository.delete(post);
    }

    private StudyPost findPost(Long postId) {
        return studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
    }

    private void validateAuthor(StudyPost post, Long userId) {
        if (!post.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_POST_AUTHOR);
        }
    }
}
