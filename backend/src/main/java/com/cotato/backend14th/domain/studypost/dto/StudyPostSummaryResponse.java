package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// 모집글 목록의 한 항목이에요.
public record StudyPostSummaryResponse(
        Long postId,
        String title,
        StudyStatus studyStatus,
        AuthorResponse author,
        long commentCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

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
