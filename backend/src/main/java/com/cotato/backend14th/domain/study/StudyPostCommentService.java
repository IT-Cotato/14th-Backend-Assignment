package com.cotato.backend14th.domain.study;

import com.cotato.backend14th.domain.user.User;
import com.cotato.backend14th.domain.user.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyPostCommentService {

    private final StudyPostCommentRepository commentRepository;
    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateCommentResponse create(Long userId, Long postId, CreateCommentRequest request) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
        if (post.getStudyStatus() == StudyStatus.CLOSED) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));
        StudyPostComment saved = commentRepository.save(StudyPostComment.builder()
                .studyPost(post)
                .user(user)
                .content(request.getContent())
                .build());
        return new CreateCommentResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public CommentListResponse list(Long postId, Long userId, int page, int size) {
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }
        Page<StudyPostComment> result = commentRepository.findByStudyPostIdOrderByCreatedAtAsc(
                postId, PageRequest.of(page, size));
        return new CommentListResponse(
                result.getContent().stream().map(c -> toItem(c, userId)).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public CreateCommentResponse update(Long userId, Long commentId, CreateCommentRequest request) {
        StudyPostComment comment = getComment(commentId);
        assertAuthor(comment, userId);
        comment.update(request.getContent());
        return new CreateCommentResponse(comment.getId());
    }

    @Transactional
    public void delete(Long userId, Long commentId) {
        StudyPostComment comment = getComment(commentId);
        assertAuthor(comment, userId);
        commentRepository.delete(comment);
    }

    private CommentItemResponse toItem(StudyPostComment comment, Long userId) {
        User author = comment.getUser();
        boolean isAuthor = userId != null && author.getId().equals(userId);
        return new CommentItemResponse(
                comment.getId(), comment.getContent(),
                new AuthorDto(author.getNickname(), author.getGeneration(), author.getPart()),
                isAuthor, comment.getCreatedAt());
    }

    private StudyPostComment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void assertAuthor(StudyPostComment comment, Long userId) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
    }
}
