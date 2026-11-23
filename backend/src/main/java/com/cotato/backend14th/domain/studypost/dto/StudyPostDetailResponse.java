package com.cotato.backend14th.domain.studypost.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

// 모집글 상세예요. (docs/api-spec.md 7번) 로그인하지 않았으면 isAuthor는 false예요.
public record StudyPostDetailResponse(
        Long postId,
        String title,
        String content,
        StudyStatus studyStatus,
        AuthorResponse author,
        @JsonProperty("isAuthor") boolean isAuthor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudyPostDetailResponse of(StudyPost post, Long loginUserId) {
        return new StudyPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getStudyStatus(),
                AuthorResponse.from(post.getUser()),
                post.isWrittenBy(loginUserId),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
