package com.cotato.backend14th.domain.studypost.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public record StudyPostDetailResponse(
        Long postId,
        String title,
        String content,
        StudyStatus studyStatus,
        AuthorResponse author,
        @JsonProperty("isAuthor")
        boolean isAuthor,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    public static StudyPostDetailResponse of(StudyPost post, Long loginUserId) {
        return new StudyPostDetailResponse(post.getId(), post.getTitle(), post.getContent(),
                post.getStudyStatus(), AuthorResponse.from(post.getUser()),
                loginUserId != null && post.isWrittenBy(loginUserId),
                post.getCreatedAt(), post.getUpdatedAt());
    }
}
