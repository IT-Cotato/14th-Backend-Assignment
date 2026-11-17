package com.cotato.backend14th.domain.studypost.dto.response;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.user.dto.response.AuthorResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyPostListItemResponse {
    private Long postId;
    private String title;
    private String studyStatus;
    private AuthorResponse author;
    private int commentCount;
    private LocalDateTime createdAt;
}
