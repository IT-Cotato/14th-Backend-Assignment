package com.cotato.backend14th.domain.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.comment.dto.CommentIdResponse;
import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.CommentRepository;
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
@Transactional(readOnly = true)
public class CommentService {

    // 모집글 목록과 같은 기준으로 한 번에 가져오는 개수를 제한해요
    private static final int MAX_PAGE_SIZE = 100;

    private final CommentRepository commentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentIdResponse create(Long postId, Long userId, CommentRequest request) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        // 모집이 마감된 글에는 신청(댓글)할 수 없어요
        if (post.getStudyStatus() == StudyStatus.CLOSED) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPostComment comment = StudyPostComment.builder()
                .studyPost(post)
                .user(user)
                .content(request.getContent())
                .build();

        return new CommentIdResponse(commentRepository.save(comment).getId());
    }

    // userId는 로그인하지 않았으면 null이에요
    public PageResponse<CommentResponse> getList(Long postId, Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }

        Page<StudyPostComment> comments =
                commentRepository.findPageByPostId(postId, PageRequest.of(page, size));

        return PageResponse.from(comments.map(c -> CommentResponse.of(c, c.isWrittenBy(userId))));
    }

    // 마감된 글의 댓글도 수정·삭제는 할 수 있어요 (명세는 작성만 막아요)
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
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateAuthor(StudyPostComment comment, Long userId) {
        if (!comment.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
    }
}
