package com.cotato.backend14th.domain.study;

import com.cotato.backend14th.domain.user.Part;
import com.cotato.backend14th.domain.user.User;
import com.cotato.backend14th.domain.user.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyPostService {

    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateStudyPostResponse create(Long userId, CreateStudyPostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));
        StudyPost post = StudyPost.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        return new CreateStudyPostResponse(studyPostRepository.save(post).getId());
    }

    @Transactional(readOnly = true)
    public StudyPostListResponse list(String keyword, String status, String part, int page, int size) {
        StudyStatus studyStatus = parseStatus(status);
        Part authorPart = parsePart(part);
        Page<StudyPost> result = studyPostRepository.search(
                keyword, studyStatus, authorPart,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new StudyPostListResponse(
                result.getContent().stream().map(this::toListItem).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public StudyPostDetailResponse get(Long postId, Long userId) {
        StudyPost post = getPost(postId);
        boolean isAuthor = userId != null && post.getUser().getId().equals(userId);
        User author = post.getUser();
        return new StudyPostDetailResponse(
                post.getId(), post.getTitle(), post.getContent(), post.getStudyStatus(),
                new AuthorDto(author.getNickname(), author.getGeneration(), author.getPart()),
                isAuthor, post.getCreatedAt(), post.getUpdatedAt());
    }

    @Transactional
    public UpdateStudyPostResponse update(Long userId, Long postId, UpdateStudyPostRequest request) {
        StudyPost post = getPost(postId);
        assertAuthor(post, userId);
        post.update(request.getTitle(), request.getContent(), request.getStudyStatus());
        return new UpdateStudyPostResponse(post.getId());
    }

    @Transactional
    public void delete(Long userId, Long postId) {
        StudyPost post = getPost(postId);
        assertAuthor(post, userId);
        studyPostRepository.delete(post);
    }

    private StudyPostListItemResponse toListItem(StudyPost post) {
        User author = post.getUser();
        return new StudyPostListItemResponse(
                post.getId(), post.getTitle(), post.getStudyStatus(),
                new AuthorDto(author.getNickname(), author.getGeneration(), author.getPart()),
                0L, post.getCreatedAt());
    }

    private StudyPost getPost(Long postId) {
        return studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
    }

    private void assertAuthor(StudyPost post, Long userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new Backend14thException(ErrorCode.NOT_POST_AUTHOR);
        }
    }

    private StudyStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return StudyStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
    }

    private Part parsePart(String part) {
        if (part == null || part.isBlank()) return null;
        try {
            return Part.valueOf(part);
        } catch (IllegalArgumentException e) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
    }
}
