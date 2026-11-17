package com.cotato.backend14th.domain.study.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // 댓글(스터디 신청)은 아직 구현 전이라 댓글 수는 항상 0이에요. 댓글 기능을 만들 때 실제 개수로 바꿔요.
        return PageResponse.from(posts.map(post -> StudyPostSummaryResponse.from(post, 0L)));
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
        studyPostRepository.delete(post);
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
