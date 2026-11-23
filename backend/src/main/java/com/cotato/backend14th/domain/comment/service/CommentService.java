package com.cotato.backend14th.domain.comment.service;

import java.util.List;

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

    private final StudyPostCommentRepository studyPostCommentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    // 확인 순서: 회원(401) → 글 조회(404) → 마감 여부(409)
    @Transactional
    public CommentIdResponse create(Long postId, Long userId, CommentRequest request) {
        // 토큰은 유효한데 회원이 없으면(탈퇴 등) 인증 실패로 봐요 (StudyPostService와 같은 기준)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        if (post.getStudyStatus() == StudyStatus.CLOSED) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }

        StudyPostComment comment = StudyPostComment.create(request.getContent(), user, post);
        return new CommentIdResponse(studyPostCommentRepository.save(comment).getId());
    }

    // 쿼리는 글 존재 확인, 목록(작성자 fetch join), 전체 개수 이렇게 3번만 나가요
    public PageResponse<CommentResponse> getList(Long postId, Long userId, int page, int size) {
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }

        Page<StudyPostComment> comments =
                studyPostCommentRepository.findAllWithUserByStudyPostId(postId, PageRequest.of(page, size));

        // 비로그인이면 userId가 null이라 isAuthor는 false예요
        List<CommentResponse> content = comments.getContent().stream()
                .map(comment -> CommentResponse.of(comment, comment.isAuthor(userId)))
                .toList();

        return PageResponse.of(comments, content);
    }

    // 확인 순서: 댓글 조회(404) → 작성자 확인(403)
    @Transactional
    public CommentIdResponse update(Long commentId, Long userId, CommentRequest request) {
        StudyPostComment comment = getAuthorizedComment(commentId, userId);
        comment.update(request.getContent());
        return new CommentIdResponse(comment.getId());
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        StudyPostComment comment = getAuthorizedComment(commentId, userId);
        studyPostCommentRepository.delete(comment);
    }

    private StudyPostComment getAuthorizedComment(Long commentId, Long userId) {
        StudyPostComment comment = studyPostCommentRepository.findById(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.isAuthor(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        return comment;
    }
}
