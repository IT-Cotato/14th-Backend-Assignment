// CommentService.java
package com.cotato.backend14th.domain.comment.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final StudyPostCommentRepository studyPostCommentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserService userService;

    // 모집이 마감된 글에는 댓글(신청)을 쓸 수 없어요
    @Transactional
    public CommentIdResponse create(Long postId, Long userId, CommentRequest request) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
        if (post.isClosed()) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }

        User user = userService.getUser(userId);
        StudyPostComment comment = studyPostCommentRepository.save(
                new StudyPostComment(post, user, request.getContent()));
        return new CommentIdResponse(comment.getId());
    }

    // 오래된 순으로 조회해요. 로그인하지 않았으면 loginUserId가 null이라 isAuthor는 false예요
    public PageResponse<CommentResponse> getList(Long postId, Long loginUserId, int page, int size) {
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"))
        );
        Page<StudyPostComment> comments = studyPostCommentRepository.findPageByStudyPostId(postId, pageable);

        List<CommentResponse> content = comments.getContent().stream()
                .map(comment -> CommentResponse.of(comment, loginUserId))
                .toList();

        return PageResponse.of(comments, content);
    }

    @Transactional
    public CommentIdResponse update(Long commentId, Long userId, CommentRequest request) {
        StudyPostComment comment = getComment(commentId);
        validateAuthor(comment, userId);
        comment.updateContent(request.getContent());
        return new CommentIdResponse(comment.getId());
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        StudyPostComment comment = getComment(commentId);
        validateAuthor(comment, userId);
        studyPostCommentRepository.delete(comment);
    }

    private StudyPostComment getComment(Long commentId) {
        return studyPostCommentRepository.findWithUserById(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateAuthor(StudyPostComment comment, Long userId) {
        if (!comment.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
    }
}
