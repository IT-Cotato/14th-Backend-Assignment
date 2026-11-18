package com.cotato.backend14th.domain.studypost.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostIdResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostListResponse;
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
public class StudyPostService {

    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository commentRepository;
    private final UserRepository userRepository;

    // 공부 포스트 생성 -> 만들어진 포스트 Id를 반환한다.
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

    @Transactional(readOnly = true)
    public PageResponse<StudyPostListResponse> getList(String keyword, StudyStatus status, Part part,
                                                       int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
        // 공백뿐인 키워드는 검색 조건으로 쓰지 않는다
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<StudyPost> posts = studyPostRepository.search(normalizedKeyword, status, part, PageRequest.of(page, size));

        // 글마다 댓글 수를 한 번의 쿼리로 센다 (글마다 따로 세면 쿼리가 글 수만큼 늘어난다)
        List<Long> postIds = posts.getContent().stream().map(StudyPost::getId).toList();
        Map<Long, Long> commentCounts = postIds.isEmpty()
                ? Map.of()
                : commentRepository.countByPostIds(postIds).stream()
                        .collect(Collectors.toMap(v -> v.getPostId(), v -> v.getCommentCount()));

        Page<StudyPostListResponse> result = posts.map(
                post -> StudyPostListResponse.of(post, commentCounts.getOrDefault(post.getId(), 0L)));
        return new PageResponse<>(result);
    }

    // userId는 로그인하지 않았으면 null이다
    @Transactional(readOnly = true)
    public StudyPostDetailResponse getDetail(Long postId, Long userId) {
        StudyPost post = studyPostRepository.findWithUserById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        boolean isAuthor = userId != null && userId.equals(post.getUser().getId());
        return new StudyPostDetailResponse(post, isAuthor);
    }

    @Transactional
    public StudyPostIdResponse update(Long postId, Long userId, StudyPostUpdateRequest request) {
        StudyPost post = getPostAsAuthor(postId, userId);
        post.update(request.getTitle(), request.getContent(), request.getStudyStatus());
        return new StudyPostIdResponse(post.getId());
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        StudyPost post = getPostAsAuthor(postId, userId);
        commentRepository.deleteByStudyPostId(post.getId());
        studyPostRepository.delete(post);
    }

    // 글이 없으면 404, 작성자가 아니면 403
    private StudyPost getPostAsAuthor(Long postId, Long userId) {
        StudyPost post = studyPostRepository.findWithUserById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
        if (!post.getUser().getId().equals(userId)) {
            throw new Backend14thException(ErrorCode.NOT_POST_AUTHOR);
        }
        return post;
    }
}
