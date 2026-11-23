package com.cotato.backend14th.domain.study.service;

import com.cotato.backend14th.domain.study.dto.CommentDtos.*;
import com.cotato.backend14th.domain.study.entity.*;
import com.cotato.backend14th.domain.study.repository.*;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cotato.backend14th.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final StudyPostRepository posts;
    private final CommentRepository comments;
    private final UserRepository users;

    @Transactional
    public Id create(long postId, long userId, Input request) {
        StudyPost post = posts.findForUpdate(postId).orElseThrow(() -> new Backend14thException(STUDY_POST_NOT_FOUND));
        if (post.getStudyStatus() == StudyStatus.CLOSED) throw new Backend14thException(STUDY_POST_CLOSED);
        return new Id(comments.save(new StudyPostComment(post, users.getReferenceById(userId), request.content())).getId());
    }

    public PageResponse<Item> list(long postId, Long viewer, int page, int size) {
        if (!posts.existsById(postId)) throw new Backend14thException(STUDY_POST_NOT_FOUND);
        return PageResponse.from(comments.findByPostId(postId, PageRequest.of(page, size, Sort.by("createdAt", "id")))
                .map(comment -> Item.from(comment, viewer)));
    }

    @Transactional
    public Id update(long id, long user, Input input) {
        ownedComment(id, user).updateContent(input.content());
        return new Id(id);
    }

    @Transactional
    public void delete(long id, long user) {
        comments.delete(ownedComment(id, user));
    }

    private StudyPostComment ownedComment(long id, long user) {
        StudyPostComment comment = comments.findForUpdate(id).orElseThrow(() -> new Backend14thException(COMMENT_NOT_FOUND));
        if (!comment.getAuthor().getId().equals(user)) throw new Backend14thException(NOT_COMMENT_AUTHOR);
        return comment;
    }
}
