package com.cotato.backend14th.domain.comment.service;

import com.cotato.backend14th.domain.comment.dto.CommentCreateRequest;
import com.cotato.backend14th.domain.comment.dto.CommentCreateResponse;
import com.cotato.backend14th.domain.comment.dto.CommentPageResponse;
import com.cotato.backend14th.domain.comment.dto.CommentUpdateRequest;
import com.cotato.backend14th.domain.comment.dto.CommentUpdateResponse;
import com.cotato.backend14th.domain.comment.dto.CommentPageResponse.Item;
import com.cotato.backend14th.domain.studypost.dto.StudyPostPageResponse.Author;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.service.CurrentUserService;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyPostCommentService {

    private final CurrentUserService currentUserService;
    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository commentRepository;

    @Transactional(readOnly = true)
    public CommentPageResponse list(Long postId, Jwt viewer, Pageable pageable) {
        if (!studyPostRepository.existsById(postId)) {
            throw new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND);
        }

        String subject = viewer == null ? null : viewer.getSubject();
        Page<Item> page = commentRepository.findPageByPostId(postId, pageable)
                .map(row -> new Item(row.getCommentId(), row.getContent(),
                        new Author(row.getNickname(), row.getGeneration(), row.getPart()),
                        row.getAuthorId().toString().equals(subject), row.getCreatedAt()));
        return new CommentPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public CommentUpdateResponse update(Long commentId, Jwt jwt, CommentUpdateRequest request) {
        StudyPostComment comment = requireOwnedCommentForUpdate(commentId, jwt);
        comment.updateContent(request.content());
        return new CommentUpdateResponse(comment.getId());
    }

    @Transactional
    public void delete(Long commentId, Jwt jwt) {
        commentRepository.delete(requireOwnedCommentForUpdate(commentId, jwt));
    }

    private StudyPostComment requireOwnedCommentForUpdate(Long commentId, Jwt jwt) {
        User user = currentUserService.requireUser(jwt);
        StudyPostComment comment = commentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new Backend14thException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        return comment;
    }

    @Transactional
    public CommentCreateResponse create(Long postId, Jwt jwt, CommentCreateRequest request) {
        User author = currentUserService.requireUser(jwt);
        StudyPost post = studyPostRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
        if (post.getStudyStatus() != StudyStatus.RECRUITING) {
            throw new Backend14thException(ErrorCode.STUDY_POST_CLOSED);
        }

        StudyPostComment comment = commentRepository.save(new StudyPostComment(request.content(), post, author));
        return new CommentCreateResponse(comment.getId());
    }
}
