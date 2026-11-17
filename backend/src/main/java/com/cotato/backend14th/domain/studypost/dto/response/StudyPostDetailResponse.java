package com.cotato.backend14th.domain.studypost.dto.response;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.user.dto.response.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyPostDetailResponse {
    private Long postId;
    private String title;
    private String content;
    private String studyStatus;
    private AuthorResponse author;
    private boolean isAuthor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 메서드명을 isAuthor()로 두면 Jackson이 이를 "author" 프로퍼티로 암묵 해석해
    // 실제 author(AuthorResponse) 필드와 충돌해 덮어써버려서, getIsAuthor()로 이름을 바꾸고
    // @JsonProperty로 JSON 키를 "isAuthor"로 고정해요.
    @JsonProperty("isAuthor")
    public boolean getIsAuthor() {
        return isAuthor;
    }
}
