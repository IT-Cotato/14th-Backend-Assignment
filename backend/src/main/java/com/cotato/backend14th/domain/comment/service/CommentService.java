package com.cotato.backend14th.domain.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.comment.dto.request.CommentCreateRequest;
import com.cotato.backend14th.domain.comment.dto.request.CommentUpdateRequest;
import com.cotato.backend14th.domain.comment.dto.response.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.response.CommentListItemResponse;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.dto.response.AuthorResponse;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final StudyPostCommentRepository studyPostCommentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentIdResponse create(Long postId, Long userId, CommentCreateRequest request) {
        StudyPost studyPost = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        if (studyPost.getStudyStatus() == StudyStatus.CLOSED) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPostComment comment = new StudyPostComment(null, studyPost, user, request.getContent());
        StudyPostComment saved = studyPostCommentRepository.save(comment);
        return new CommentIdResponse(saved.getId());
    }

    public PageResponse<CommentListItemResponse> list(Long postId, Long userId, int page, int size) {
        if (page < 0 || size < 1) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }

        Page<StudyPostComment> result = studyPostCommentRepository.findByStudyPostIdWithUser(
                postId, PageRequest.of(page, size)
        );

        Page<CommentListItemResponse> mapped = result.map(comment -> toListItem(comment, userId));
        return PageResponse.of(mapped);
    }

    @Transactional
    public CommentIdResponse update(Long commentId, Long userId, CommentUpdateRequest request) {
        StudyPostComment comment = studyPostCommentRepository.findByIdWithUser(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));

        requireAuthor(comment, userId);

        comment.updateContent(request.getContent());
        return new CommentIdResponse(comment.getId());
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        StudyPostComment comment = studyPostCommentRepository.findByIdWithUser(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));

        requireAuthor(comment, userId);

        studyPostCommentRepository.delete(comment);
    }

    private void requireAuthor(StudyPostComment comment, Long userId) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
    }

    private CommentListItemResponse toListItem(StudyPostComment comment, Long userId) {
        boolean isAuthor = userId != null && userId.equals(comment.getUser().getId());
        return new CommentListItemResponse(
                comment.getId(),
                comment.getContent(),
                AuthorResponse.from(comment.getUser()),
                isAuthor,
                comment.getCreatedAt()
        );
    }
}
