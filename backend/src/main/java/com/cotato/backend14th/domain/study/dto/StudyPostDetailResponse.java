package com.cotato.backend14th.domain.study.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyPostDetailResponse {

    private final Long postId;
    private final String title;
    private final String content;
    private final StudyStatus studyStatus;
    private final AuthorResponse author;

    // 자바 빈 규칙상 isAuthor() 게터는 JSON에서 "author"가 되어 위 author와 겹치므로 이름을 명시해요
    @JsonProperty("isAuthor")
    private final boolean mine;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime updatedAt;

    public static StudyPostDetailResponse of(StudyPost post, Long currentUserId) {
        return new StudyPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getStudyStatus(),
                AuthorResponse.from(post.getUser()),
                post.isWrittenBy(currentUserId),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
