package com.cotato.backend14th.domain.studypost.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentCountProjection;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.studypost.dto.request.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.request.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.dto.response.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.response.StudyPostIdResponse;
import com.cotato.backend14th.domain.studypost.dto.response.StudyPostListItemResponse;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.dto.response.AuthorResponse;
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
    private final UserRepository userRepository;
    private final StudyPostCommentRepository studyPostCommentRepository;

    @Transactional
    public StudyPostIdResponse create(Long userId, StudyPostCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPost studyPost = new StudyPost(
                null, user, request.getTitle(), request.getContent(), StudyStatus.RECRUITING
        );
        StudyPost saved = studyPostRepository.save(studyPost);
        return new StudyPostIdResponse(saved.getId());
    }

    public PageResponse<StudyPostListItemResponse> search(
            String keyword, String status, String part, int page, int size
    ) {
        if (page < 0 || size < 1) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }

        StudyStatus statusEnum = parseStatus(status);
        Part partEnum = parsePart(part);
        String keywordOrNull = (keyword == null || keyword.isBlank()) ? null : keyword;

        Page<StudyPost> result = studyPostRepository.search(
                keywordOrNull, statusEnum, partEnum, PageRequest.of(page, size)
        );

        List<Long> postIds = result.getContent().stream().map(StudyPost::getId).toList();
        Map<Long, Long> commentCountMap = studyPostCommentRepository.countByStudyPostIdIn(postIds).stream()
                .collect(Collectors.toMap(
                        StudyPostCommentCountProjection::getStudyPostId,
                        StudyPostCommentCountProjection::getCommentCount
                ));

        Page<StudyPostListItemResponse> mapped = result.map(sp -> toListItem(sp, commentCountMap));
        return PageResponse.of(mapped);
    }

    public StudyPostDetailResponse getDetail(Long postId, Long userId) {
        StudyPost studyPost = studyPostRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        boolean isAuthor = userId != null && userId.equals(studyPost.getUser().getId());

        return new StudyPostDetailResponse(
                studyPost.getId(),
                studyPost.getTitle(),
                studyPost.getContent(),
                studyPost.getStudyStatus().name(),
                AuthorResponse.from(studyPost.getUser()),
                isAuthor,
                studyPost.getCreatedAt(),
                studyPost.getUpdatedAt()
        );
    }

    @Transactional
    public StudyPostIdResponse update(Long postId, Long userId, StudyPostUpdateRequest request) {
        StudyPost studyPost = studyPostRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        requireAuthor(studyPost, userId);

        if (request.getTitle() != null) {
            studyPost.updateTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            studyPost.updateContent(request.getContent());
        }
        if (request.getStudyStatus() != null) {
            studyPost.updateStatus(parseStatus(request.getStudyStatus()));
        }

        return new StudyPostIdResponse(studyPost.getId());
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        StudyPost studyPost = studyPostRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        requireAuthor(studyPost, userId);

        studyPostCommentRepository.deleteByStudyPostId(postId);
        studyPostRepository.delete(studyPost);
    }

    private void requireAuthor(StudyPost studyPost, Long userId) {
        if (!studyPost.getUser().getId().equals(userId)) {
            throw new Backend14thException(ErrorCode.NOT_POST_AUTHOR);
        }
    }

    private StudyStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return StudyStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
    }

    private Part parsePart(String part) {
        if (part == null || part.isBlank()) {
            return null;
        }
        try {
            return Part.valueOf(part);
        } catch (IllegalArgumentException e) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
    }

    private StudyPostListItemResponse toListItem(StudyPost studyPost, Map<Long, Long> commentCountMap) {
        long commentCount = commentCountMap.getOrDefault(studyPost.getId(), 0L);
        return new StudyPostListItemResponse(
                studyPost.getId(),
                studyPost.getTitle(),
                studyPost.getStudyStatus().name(),
                AuthorResponse.from(studyPost.getUser()),
                (int) commentCount,
                studyPost.getCreatedAt()
        );
    }
}
