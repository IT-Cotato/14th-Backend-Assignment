package com.cotato.backend14th.domain.study.dto;

import com.cotato.backend14th.domain.study.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthDtos.Author;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class CommentDtos {
    private CommentDtos() {}

    public record Input(@NotBlank @Size(max = 500) String content) {}
    public record Id(long commentId) {}
    public record Item(long commentId, String content, Author author, boolean isAuthor, LocalDateTime createdAt) {
        public static Item from(StudyPostComment comment, Long viewer) {
            return new Item(comment.getId(), comment.getContent(), Author.from(comment.getAuthor()),
                    comment.getAuthor().getId().equals(viewer), comment.getCreatedAt());
        }
    }
}
