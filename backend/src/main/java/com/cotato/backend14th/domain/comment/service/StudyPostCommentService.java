package com.cotato.backend14th.domain.comment.service;

import org.springframework.data.domain.PageRequest;
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
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

// 댓글이 곧 스터디 신청이에요. 같은 사람이 여러 번 달아도 돼요.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyPostCommentService {

    private final StudyPostCommentRepository commentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    // 모집이 마감된 글에는 댓글을 쓸 수 없어요.
    @Transactional
    public CommentIdResponse create(Long postId, Long userId, CommentRequest request) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
        if (post.isClosed()) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }
        // 토큰은 유효하지만 사용자가 없는 경우도 인증 실패로 봐요
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPostComment comment = StudyPostComment.builder()
                .studyPost(post)
                .user(user)
                .content(request.content())
                .build();
        return new CommentIdResponse(commentRepository.save(comment).getId());
    }

    // 오래된 순이에요. 작성자를 함께 가져와서 N+1이 나지 않아요.
    public PageResponse<CommentResponse> getComments(Long postId, Long loginUserId, int page, int size) {
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }
        return PageResponse.of(
                commentRepository.findPageByPostId(postId, PageRequest.of(page, size)),
                comment -> CommentResponse.of(comment, loginUserId));
    }

    @Transactional
    public CommentIdResponse update(Long commentId, Long userId, CommentRequest request) {
        StudyPostComment comment = findOwnedComment(commentId, userId);
        comment.updateContent(request.content());
        return new CommentIdResponse(comment.getId());
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        commentRepository.delete(findOwnedComment(commentId, userId));
    }

    private StudyPostComment findOwnedComment(Long commentId, Long userId) {
        StudyPostComment comment = commentRepository.findWithUserById(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        return comment;
    }
}
