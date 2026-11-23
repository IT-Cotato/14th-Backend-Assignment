package com.cotato.backend14th.domain.studypost.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
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

    // 작성자 여부. getter를 만들지 않고 필드에 JSON 이름을 직접 붙여요.
    // isAuthor() getter가 있으면 Jackson이 getAuthor()와 같은 "author" 속성으로 묶어서
    // 위의 author 객체가 응답에서 빠져버려요.
    @Getter(AccessLevel.NONE)
    @JsonProperty("isAuthor")
    private boolean isAuthor;

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
