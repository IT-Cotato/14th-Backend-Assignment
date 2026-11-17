package com.cotato.backend14th.domain.studypost.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Page<StudyPostListItemResponse> mapped = result.map(this::toListItem);
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

        // TODO: 3단계에서 StudyPostComment 추가 시 이 글에 달린 댓글도 함께 삭제해야 함
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

    private StudyPostListItemResponse toListItem(StudyPost studyPost) {
        return new StudyPostListItemResponse(
                studyPost.getId(),
                studyPost.getTitle(),
                studyPost.getStudyStatus().name(),
                AuthorResponse.from(studyPost.getUser()),
                0,
                studyPost.getCreatedAt()
        );
    }
}
