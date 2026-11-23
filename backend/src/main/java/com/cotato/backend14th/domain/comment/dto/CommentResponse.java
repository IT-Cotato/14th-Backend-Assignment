package com.cotato.backend14th.domain.comment.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;

import lombok.Getter;

@Getter
public class CommentResponse {

    private final Long commentId;
    private final String content;
    private final AuthorResponse author;
    // JSON 이름이 명세대로 "isAuthor"가 되도록 Boolean(getIsAuthor)로 둔다
    private final Boolean isAuthor;
    private final LocalDateTime createdAt;

    public CommentResponse(StudyPostComment comment, boolean isAuthor) {
        this.commentId = comment.getId();
        this.content = comment.getContent();
        this.author = AuthorResponse.from(comment.getUser());
        this.isAuthor = isAuthor;
        this.createdAt = comment.getCreatedAt();
    }
}
