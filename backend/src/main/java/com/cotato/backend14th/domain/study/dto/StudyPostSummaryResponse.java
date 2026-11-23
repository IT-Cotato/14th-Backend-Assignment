package com.cotato.backend14th.domain.study.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 모집글 목록의 한 항목이에요
@Getter
@AllArgsConstructor
public class StudyPostSummaryResponse {

    @Schema(description = "모집글 ID", example = "10")
    private Long postId;

    @Schema(description = "제목", example = "알고리즘 스터디 모집")
    private String title;

    @Schema(description = "모집 상태", example = "RECRUITING")
    private StudyStatus studyStatus;

    @Schema(description = "작성자")
    private AuthorResponse author;

    @Schema(description = "댓글 수", example = "3")
    private long commentCount;

    @Schema(description = "작성 시각", example = "2026-11-08T12:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

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
