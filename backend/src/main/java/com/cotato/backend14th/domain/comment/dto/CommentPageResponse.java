package com.cotato.backend14th.domain.comment.dto;

import com.cotato.backend14th.domain.studypost.dto.StudyPostPageResponse.Author;

import java.time.LocalDateTime;
import java.util.List;

public record CommentPageResponse(List<Item> content, int page, int size, long totalElements, int totalPages) {

    public record Item(Long commentId, String content, Author author, boolean isAuthor, LocalDateTime createdAt) {
    }
}
