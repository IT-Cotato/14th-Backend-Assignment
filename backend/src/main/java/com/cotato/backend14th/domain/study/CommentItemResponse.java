package com.cotato.backend14th.domain.study;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentItemResponse {
    private Long commentId;
    private String content;
    private AuthorDto author;
    @JsonProperty("isAuthor")
    private boolean isAuthor;
    private LocalDateTime createdAt;
}