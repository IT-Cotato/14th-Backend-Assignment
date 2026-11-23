package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.dto.StudyPostPageResponse.Author;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;

import java.time.LocalDateTime;

public record StudyPostDetailResponse(
        Long postId, String title, String content, StudyStatus studyStatus, Author author,
        boolean isAuthor, LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
