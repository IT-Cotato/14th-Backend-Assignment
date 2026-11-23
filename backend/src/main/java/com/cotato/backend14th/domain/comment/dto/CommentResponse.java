package com.cotato.backend14th.domain.comment.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentResponse {

    private Long commentId;
    private String content;
    private AuthorResponse author;

    // boolean이면 Lombok getter가 isAuthor()가 되어 JSON 이름이 "author"로 바뀌고 위 author와 충돌해요 (StudyPostDetailResponse와 같은 이유)
    private Boolean isAuthor;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static CommentResponse of(StudyPostComment comment, boolean isAuthor) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                AuthorResponse.from(comment.getUser()),
                isAuthor,
                comment.getCreatedAt()
        );
    }
}
