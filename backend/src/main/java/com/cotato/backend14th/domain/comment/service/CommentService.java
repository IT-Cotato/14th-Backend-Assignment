package com.cotato.backend14th.domain.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.study.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final StudyPostCommentRepository commentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentIdResponse create(Long postId, Long userId, CommentRequest request) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        // 모집이 마감된 글에는 댓글(스터디 신청)을 쓸 수 없어요. 수정·삭제는 막지 않아요.
        if (post.getStudyStatus() == StudyStatus.CLOSED) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }

        // 토큰은 유효한데 사용자가 없는 경우도 인증 실패로 처리해요
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPostComment saved = commentRepository.save(StudyPostComment.create(post, user, request.getContent()));
        return new CommentIdResponse(saved.getId());
    }

    // currentUserId는 로그인하지 않았으면 null이에요
    public PageResponse<CommentResponse> getList(Long postId, Long currentUserId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }

        Page<StudyPostComment> comments = commentRepository.findByPostId(postId, PageRequest.of(page, size));
        return PageResponse.from(comments.map(comment -> CommentResponse.of(comment, currentUserId)));
    }

    @Transactional
    public CommentIdResponse update(Long commentId, Long userId, CommentRequest request) {
        StudyPostComment comment = findComment(commentId);
        validateAuthor(comment, userId);

        comment.updateContent(request.getContent());
        return new CommentIdResponse(comment.getId());
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        StudyPostComment comment = findComment(commentId);
        validateAuthor(comment, userId);
        commentRepository.delete(comment);
    }

    private StudyPostComment findComment(Long commentId) {
        return commentRepository.findWithUserById(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateAuthor(StudyPostComment comment, Long userId) {
        if (!comment.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
    }
}
