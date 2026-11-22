package com.cotato.backend14th.domain.studypost.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 목록 API(6번)의 항목 하나
@Getter
@AllArgsConstructor
public class StudyPostSummaryResponse {

    private Long postId;
    private String title;
    private StudyStatus studyStatus;
    private AuthorResponse author;
    private long commentCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static StudyPostSummaryResponse of(StudyPost post, long commentCount) {
        return new StudyPostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getStudyStatus(),
                AuthorResponse.from(post.getUser()),
                commentCount,
                post.getCreatedAt()
        );
    }
}
