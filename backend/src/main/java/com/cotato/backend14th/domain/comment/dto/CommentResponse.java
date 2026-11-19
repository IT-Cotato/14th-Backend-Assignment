package com.cotato.backend14th.domain.comment.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 댓글 목록의 항목 하나
@Getter
@AllArgsConstructor
public class CommentResponse {

    private Long commentId;
    private String content;
    private AuthorResponse author;
    private boolean isAuthor;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // Lombok이 만드는 isAuthor()는 JSON에서 "author"로 읽혀 위의 author와 충돌해요.
    // 그래서 getter를 직접 쓰고 JSON 이름을 "isAuthor"로 고정해요. (StudyPostDetailResponse와 같은 방식)
    @JsonProperty("isAuthor")
    public boolean isAuthor() {
        return isAuthor;
    }

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
