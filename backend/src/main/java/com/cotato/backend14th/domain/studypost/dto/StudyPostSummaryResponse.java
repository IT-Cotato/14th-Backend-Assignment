package com.cotato.backend14th.domain.studypost.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;

// 모집글 목록의 항목이에요. (docs/api-spec.md 6번)
public record StudyPostSummaryResponse(
        Long postId,
        String title,
        StudyStatus studyStatus,
        AuthorResponse author,
        long commentCount,
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
