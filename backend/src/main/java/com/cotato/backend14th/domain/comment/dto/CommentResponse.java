package com.cotato.backend14th.domain.comment.dto;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

// 댓글 목록의 한 항목이에요.
public record CommentResponse(
        Long commentId,
        String content,
        AuthorResponse author,
        @JsonProperty("isAuthor")
        boolean isAuthor,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static CommentResponse of(StudyPostComment comment, Long viewerId) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                AuthorResponse.from(comment.getUser()),
                comment.isWrittenBy(viewerId),
                comment.getCreatedAt()
        );
    }
}
