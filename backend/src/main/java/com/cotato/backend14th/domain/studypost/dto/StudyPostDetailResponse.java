package com.cotato.backend14th.domain.studypost.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 모집글 상세
@Getter
@AllArgsConstructor
public class StudyPostDetailResponse {

    private Long postId;
    private String title;
    private String content;
    private StudyStatus studyStatus;
    private AuthorResponse author;
    private boolean isAuthor;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // Lombok이 만드는 isAuthor()는 JSON에서 "author"로 읽혀 위의 author와 충돌해요.
    // 그래서 getter를 직접 쓰고 JSON 이름을 "isAuthor"로 고정해요.
    @JsonProperty("isAuthor")
    public boolean isAuthor() {
        return isAuthor;
    }

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
