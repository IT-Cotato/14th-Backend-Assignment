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
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudyPostCommentService {

    private final StudyPostCommentRepository commentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentIdResponse create(Long postId, Long userId, CommentRequest request) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        // 마감된 글에는 신청(댓글)할 수 없어요. 수정·삭제는 명세상 마감 제한이 없어서 막지 않아요
        if (post.getStudyStatus() == StudyStatus.CLOSED) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        // 같은 사람이 여러 번 댓글을 달아도 돼요 (api-spec 10번)
        StudyPostComment comment = StudyPostComment.builder()
                .studyPost(post)
                .user(user)
                .content(request.getContent())
                .build();

        return new CommentIdResponse(commentRepository.save(comment).getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getList(Long postId, Long loginUserId, int page, int size) {
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }

        return PageResponse.from(
                commentRepository.findPageByStudyPostId(postId, PageRequest.of(page, size))
                        // 로그인하지 않았으면 loginUserId가 null이라 모두 false예요
                        .map(comment -> CommentResponse.of(comment, comment.isWrittenBy(loginUserId)))
        );
    }

    @Transactional
    public CommentIdResponse update(Long commentId, Long userId, CommentRequest request) {
        StudyPostComment comment = getCommentWrittenBy(commentId, userId);
        comment.updateContent(request.getContent());
        return new CommentIdResponse(comment.getId());
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        commentRepository.delete(getCommentWrittenBy(commentId, userId));
    }

    // 없는 댓글이면 404를 먼저 내고, 있을 때만 작성자인지 확인해요
    private StudyPostComment getCommentWrittenBy(Long commentId, Long userId) {
        StudyPostComment comment = commentRepository.findWithUserById(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        return comment;
    }
}
