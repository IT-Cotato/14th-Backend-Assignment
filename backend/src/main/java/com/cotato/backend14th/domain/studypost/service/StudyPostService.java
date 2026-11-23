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
import com.cotato.backend14th.domain.user.service.UserService;
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
    private final UserService userService;

    @Transactional
    public StudyPostIdResponse create(Long userId, StudyPostCreateRequest request) {
        User user = userService.getUser(userId);
        StudyPost post = new StudyPost(user, request.title().trim(), request.content());
        return new StudyPostIdResponse(studyPostRepository.save(post).getId());
    }

    public PageResponse<StudyPostSummaryResponse> search(String keyword, StudyStatus status, Part part,
                                                         int page, int size) {
        Page<StudyPost> posts = studyPostRepository.search(toLikePattern(keyword), status, part,
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
        StudyPost post = getOwnedPost(postId, userId);
        post.update(
                request.title() == null ? null : request.title().trim(),
                request.content(),
                request.studyStatus());
        return new StudyPostIdResponse(post.getId());
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        StudyPost post = getOwnedPost(postId, userId);
        commentRepository.deleteAllByPostId(post.getId());
        studyPostRepository.delete(post);
    }

    public StudyPost getPost(Long postId) {
        return studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
    }

    private StudyPost getOwnedPost(Long postId, Long userId) {
        StudyPost post = getPost(postId);
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
                .collect(Collectors.toMap(CommentCount::getPostId, CommentCount::getCommentCount));
    }

    /**
     * 키워드를 LIKE 패턴으로 바꿔요. %, _ 는 글자 그대로 찾도록 '!'로 이스케이프해요.
     */
    static String toLikePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String escaped = keyword.trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
