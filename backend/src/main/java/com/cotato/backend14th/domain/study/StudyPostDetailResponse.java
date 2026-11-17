package com.cotato.backend14th.domain.study;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyPostDetailResponse {
    private Long postId;
    private String title;
    private String content;
    private StudyStatus studyStatus;
    private AuthorDto author;
    @JsonProperty("isAuthor")
    private boolean isAuthor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
