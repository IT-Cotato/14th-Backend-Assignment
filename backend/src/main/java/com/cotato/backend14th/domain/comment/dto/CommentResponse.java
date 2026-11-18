package com.cotato.backend14th.domain.comment.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentResponse {

    private final Long commentId;
    private final String content;
    private final AuthorResponse author;

    // 자바 빈 규칙상 isAuthor() 게터는 JSON에서 "author"가 되어 위 author와 겹치므로 이름을 명시해요
    @JsonProperty("isAuthor")
    private final boolean mine;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    // currentUserId는 로그인하지 않았으면 null이에요
    public static CommentResponse of(StudyPostComment comment, Long currentUserId) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                AuthorResponse.from(comment.getUser()),
                comment.isWrittenBy(currentUserId),
                comment.getCreatedAt()
        );
    }
}
