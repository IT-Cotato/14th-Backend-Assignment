package com.cotato.backend14th.domain.comment.service;

import com.cotato.backend14th.domain.comment.dto.CommentCreateRequest;
import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.dto.CommentUpdateRequest;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final StudyPostCommentRepository commentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    // 댓글 작성 (= 스터디 신청)
    // 회원이 없으면 401, 글이 없으면 404, 모집이 마감됐으면 409예요.
    @Transactional
    public CommentIdResponse create(Long postId, Long userId, CommentCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        if (post.getStudyStatus() == StudyStatus.CLOSED) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }

        StudyPostComment savedComment = commentRepository.save(
                new StudyPostComment(post, user, request.content())
        );

        return new CommentIdResponse(savedComment.getId());
    }

    // 댓글 목록 (오래된 순, viewerId가 null이면 비로그인 사용자예요)
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(Long postId, Long viewerId, int page, int size) {
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }

        return PageResponse.from(
                commentRepository.findPageByStudyPostId(postId, PageRequest.of(page, size))
                        .map(comment -> CommentResponse.of(comment, viewerId))
        );
    }

    // 댓글 수정 (작성자만)
    @Transactional
    public CommentIdResponse update(Long commentId, Long userId, CommentUpdateRequest request) {
        StudyPostComment comment = getCommentWrittenBy(commentId, userId);

        comment.updateContent(request.content());

        return new CommentIdResponse(comment.getId());
    }

    // 댓글 삭제 (작성자만)
    @Transactional
    public void delete(Long commentId, Long userId) {
        StudyPostComment comment = getCommentWrittenBy(commentId, userId);

        commentRepository.delete(comment);
    }

    // 댓글이 없으면 404, 작성자가 아니면 403이에요.
    private StudyPostComment getCommentWrittenBy(Long commentId, Long userId) {
        StudyPostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        return comment;
    }
}
