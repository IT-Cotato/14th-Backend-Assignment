package com.cotato.backend14th.domain.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.service.UserService;
import com.cotato.backend14th.global.common.dto.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final Sort OLDEST = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    private final StudyPostCommentRepository commentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserService userService;

    @Transactional
    public CommentIdResponse create(Long postId, Long userId, CommentRequest request) {
        StudyPost post = getPost(postId);
        if (post.isClosed()) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }
        User user = userService.getUser(userId);
        StudyPostComment comment = StudyPostComment.builder()
                .studyPost(post)
                .user(user)
                .content(request.content())
                .build();
        return new CommentIdResponse(commentRepository.save(comment).getId());
    }

    public PageResponse<CommentResponse> getComments(Long postId, Long loginUserId, int page, int size) {
        getPost(postId); // 없는 글이면 404
        Page<StudyPostComment> comments = commentRepository.findByPostId(postId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), OLDEST));
        return PageResponse.of(comments, comment -> CommentResponse.of(comment, loginUserId));
    }

    @Transactional
    public CommentIdResponse update(Long commentId, Long userId, CommentRequest request) {
        StudyPostComment comment = getOwnedComment(commentId, userId);
        comment.updateContent(request.content());
        return new CommentIdResponse(comment.getId());
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        commentRepository.delete(getOwnedComment(commentId, userId));
    }

    private StudyPost getPost(Long postId) {
        return studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
    }

    private StudyPostComment getOwnedComment(Long commentId, Long userId) {
        StudyPostComment comment = commentRepository.findWithUserById(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        return comment;
    }
}
