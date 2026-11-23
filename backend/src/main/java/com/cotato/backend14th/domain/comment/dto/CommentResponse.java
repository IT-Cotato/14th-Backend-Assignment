package com.cotato.backend14th.domain.comment.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CommentResponse(
        Long commentId,
        String content,
        AuthorResponse author,
        @JsonProperty("isAuthor")
        boolean isAuthor,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static CommentResponse of(StudyPostComment comment, Long loginUserId) {
        return new CommentResponse(comment.getId(), comment.getContent(),
                AuthorResponse.from(comment.getUser()),
                loginUserId != null && comment.isWrittenBy(loginUserId),
                comment.getCreatedAt());
    }
}
