// StudyPostDetailResponse.java
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

@Getter
@AllArgsConstructor
public class StudyPostDetailResponse {

    private final Long postId;
    private final String title;
    private final String content;
    private final StudyStatus studyStatus;
    private final AuthorResponse author;

    // Lombok이 만드는 isAuthor() getter는 JSON에서 "author"로 읽혀 작성자 정보(getAuthor)와 겹쳐요.
    // 그래서 getter를 getIsAuthor()로 직접 만들고 JSON 이름을 "isAuthor"로 고정해요
    @Getter(AccessLevel.NONE)
    private final boolean isAuthor;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime updatedAt;

    @JsonProperty("isAuthor")
    public boolean getIsAuthor() {
        return isAuthor;
    }

    public static StudyPostDetailResponse of(StudyPost post, Long loginUserId) {
        return new StudyPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getStudyStatus(),
                AuthorResponse.from(post.getUser()),
                post.isWrittenBy(loginUserId),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
