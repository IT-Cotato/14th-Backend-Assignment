package com.cotato.backend14th.domain.study.dto;

import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthDtos.Author;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class StudyDtos {
    private StudyDtos() {}

    public record Create(@NotBlank @Size(max = 50) String title,
                         @NotBlank @Size(max = 2000) String content) {}
    public record Update(@Pattern(regexp = "(?s).*\\S.*") @Size(max = 50) String title,
                         @Pattern(regexp = "(?s).*\\S.*") @Size(max = 2000) String content,
                         StudyStatus studyStatus) {}
    public record Id(long postId) {}
    public record Summary(long postId, String title, StudyStatus studyStatus, Author author,
                          long commentCount, LocalDateTime createdAt) {}
    public record Detail(long postId, String title, String content, StudyStatus studyStatus, Author author,
                         boolean isAuthor, LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static Detail from(StudyPost post, Long viewer) {
            return new Detail(post.getId(), post.getTitle(), post.getContent(), post.getStudyStatus(),
                    Author.from(post.getAuthor()), post.getAuthor().getId().equals(viewer),
                    post.getCreatedAt(), post.getUpdatedAt());
        }
    }
}
