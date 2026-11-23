package com.cotato.backend14th.domain.comment.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

// 댓글 목록의 항목이에요. (docs/api-spec.md 11번) 로그인하지 않았으면 isAuthor는 false예요.
public record CommentResponse(
        Long commentId,
        String content,
        AuthorResponse author,
        @JsonProperty("isAuthor") boolean isAuthor,
        LocalDateTime createdAt
) {
    public static CommentResponse of(StudyPostComment comment, Long loginUserId) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                AuthorResponse.from(comment.getUser()),
                comment.isWrittenBy(loginUserId),
                comment.getCreatedAt()
        );
    }
}
