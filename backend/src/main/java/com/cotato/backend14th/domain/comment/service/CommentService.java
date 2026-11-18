package com.cotato.backend14th.domain.comment.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    // 레포지토리 호출 -> 의존성 주입, 생성자 생성시에 final키워드 있는 매개변수가 생성
    private final StudyPostCommentRepository commentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    // 댓글 생성 함수 -> 반환 타입 CommentResponse DTO
    @Transactional
    public CommentIdResponse create(Long postId, Long userId, CommentRequest request) {

        // 댓글을 생성하고자 하는 post -> id 없는 경우 Error 호출
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        // 스터디가 끝난 경우
        if (post.getStudyStatus() == StudyStatus.CLOSED) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPostComment comment = StudyPostComment.builder()
                .studyPost(post).user(user).content(request.getContent()).build();

        return new CommentIdResponse(commentRepository.save(comment).getId());
    }

    // userId는 로그인하지 않았으면 null이다
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getList(Long postId, Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new Backend14thException(ErrorCode.INVALID_INPUT);
        }
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }

        // pageRequest는 페이 관련 요청서에 해당한다.
        Page<StudyPostComment> comments = commentRepository.findByPostId(postId, PageRequest.of(page, size));


        return new PageResponse<>(comments.map(c ->
                new CommentResponse(c, userId != null && userId.equals(c.getUser().getId()))));
    }

    // 댓글 Udate 메서드 : 댓글을 수정하는 메서드 반환 타입은 CommentIdResponse이다.
    @Transactional
    public CommentIdResponse update(Long commentId, Long userId, CommentRequest request){

        // 댓글 정보를 얻어옴
        StudyPostComment comment = getCommentAsAuthor(commentId, userId);
        comment.update(request.getContent());
        return new CommentIdResponse(comment.getId());
    }

    @Transactional
    public void delete(Long commentId, Long userId){
        commentRepository.delete(getCommentAsAuthor(commentId, userId));
    }

    // 댓글이 없으면 404, 작성자가 아니면 403
    private StudyPostComment getCommentAsAuthor(Long commentId, Long userId) {
        StudyPostComment comment = commentRepository.findWithUserById(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getUser().getId().equals(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        return comment;
    }

}
