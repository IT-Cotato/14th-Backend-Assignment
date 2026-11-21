// CommentResponse.java
package com.cotato.backend14th.domain.comment.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentResponse {

    private final Long commentId;
    private final String content;
    private final AuthorResponse author;

    // Lombok이 만드는 isAuthor() getter는 JSON에서 "author"로 읽혀 작성자 정보(getAuthor)와 겹쳐요.
    // 그래서 getter를 getIsAuthor()로 직접 만들고 JSON 이름을 "isAuthor"로 고정해요
    @Getter(AccessLevel.NONE)
    private final boolean isAuthor;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    @JsonProperty("isAuthor")
    public boolean getIsAuthor() {
        return isAuthor;
    }

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
