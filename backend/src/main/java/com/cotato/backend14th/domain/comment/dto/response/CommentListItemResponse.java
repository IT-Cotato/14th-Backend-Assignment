package com.cotato.backend14th.domain.comment.dto.response;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.user.dto.response.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentListItemResponse {
    private Long commentId;
    private String content;
    private AuthorResponse author;
    private boolean isAuthor;
    private LocalDateTime createdAt;

    // author(AuthorResponse) 필드와의 Jackson 프로퍼티명 충돌을 피하려고 getIsAuthor()로 이름을 두고
    // @JsonProperty로 JSON 키를 "isAuthor"로 고정해요. (StudyPostDetailResponse와 같은 이유)
    @JsonProperty("isAuthor")
    public boolean getIsAuthor() {
        return isAuthor;
    }
}
