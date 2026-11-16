package com.cotato.backend14th.domain.study.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyPostDetailResponse {

    @Schema(description = "모집글 ID", example = "10")
    private Long postId;

    @Schema(description = "제목", example = "알고리즘 스터디 모집")
    private String title;

    @Schema(description = "내용", example = "주 2회 백준 풀이...")
    private String content;

    @Schema(description = "모집 상태", example = "RECRUITING")
    private StudyStatus studyStatus;

    @Schema(description = "작성자")
    private AuthorResponse author;

    // boolean 필드의 Lombok getter(isAuthor())는 JSON에서 "author"가 돼 위 author와 겹쳐요. 이름을 직접 지정해요
    @Schema(description = "요청한 사람이 작성자인지 (비로그인이면 false)", example = "true")
    @JsonProperty("isAuthor")
    private boolean isAuthor;

    @Schema(description = "작성 시각", example = "2026-11-08T12:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각", example = "2026-11-08T12:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

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
