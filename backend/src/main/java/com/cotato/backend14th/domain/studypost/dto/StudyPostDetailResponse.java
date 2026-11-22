package com.cotato.backend14th.domain.studypost.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyPostDetailResponse {

    private Long postId;
    private String title;
    private String content;
    private StudyStatus studyStatus;
    private AuthorResponse author;

    // boolean으로 두면 Lombok getter가 isAuthor()가 되어 JSON 이름이 "author"로 바뀌고 위 author와 충돌해요.
    // Boolean이면 getIsAuthor()가 만들어져서 명세대로 "isAuthor"로 나가요
    private Boolean isAuthor;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
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
