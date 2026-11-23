package com.cotato.backend14th.domain.study.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 목록의 항목 하나예요
@Getter
@AllArgsConstructor
public class StudyPostSummaryResponse {

    private final Long postId;
    private final String title;
    private final StudyStatus studyStatus;
    private final AuthorResponse author;
    private final long commentCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    public static StudyPostSummaryResponse from(StudyPost post, long commentCount) {
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
