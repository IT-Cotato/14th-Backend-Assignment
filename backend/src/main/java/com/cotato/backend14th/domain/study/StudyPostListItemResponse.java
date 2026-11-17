package com.cotato.backend14th.domain.study;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyPostListItemResponse {
    private Long postId;
    private String title;
    private StudyStatus studyStatus;
    private AuthorDto author;
    private long commentCount;
    private LocalDateTime createdAt;
}
