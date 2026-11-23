package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

// 모집글 상세 응답이에요.
public record StudyPostDetailResponse(
        Long postId,
        String title,
        String content,
        StudyStatus studyStatus,
        AuthorResponse author,
        @JsonProperty("isAuthor")
        boolean isAuthor,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    public static StudyPostDetailResponse of(StudyPost post, boolean isAuthor) {
        return new StudyPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getStudyStatus(),
                AuthorResponse.from(post.getUser()),
                isAuthor,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
