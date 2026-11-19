package com.cotato.backend14th.domain.comment.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 댓글 목록의 항목 하나
@Getter
@AllArgsConstructor
public class CommentResponse {

    private Long commentId;
    private String content;
    private AuthorResponse author;

    // 작성자 여부. getter를 만들지 않고 필드에 JSON 이름을 직접 붙여요.
    // isAuthor() getter가 있으면 Jackson이 getAuthor()와 같은 "author" 속성으로 묶어서
    // 위의 author 객체가 응답에서 빠져버려요. (StudyPostDetailResponse와 같은 방식)
    @Getter(AccessLevel.NONE)
    @JsonProperty("isAuthor")
    private boolean isAuthor;

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
