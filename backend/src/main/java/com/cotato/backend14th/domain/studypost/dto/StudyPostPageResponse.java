package com.cotato.backend14th.domain.studypost.dto;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.entity.Part;

import java.time.LocalDateTime;
import java.util.List;

public record StudyPostPageResponse(
        List<Item> content, int page, int size, long totalElements, int totalPages
) {
    public record Item(
            Long postId, String title, StudyStatus studyStatus, Author author, long commentCount, LocalDateTime createdAt
    ) {
    }

    public record Author(String nickname, int generation, Part part) {
    }
}
