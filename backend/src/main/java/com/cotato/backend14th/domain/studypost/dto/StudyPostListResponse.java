package com.cotato.backend14th.domain.studypost.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyPostListResponse {

    private Long postId;
    private String title;
    private StudyStatus studyStatus;
    private AuthorResponse author;
    private long commentCount;
    private LocalDateTime createdAt;

    public static StudyPostListResponse of(StudyPost post, long commentCount) {
        return new StudyPostListResponse(
                post.getId(),
                post.getTitle(),
                post.getStudyStatus(),
                AuthorResponse.from(post.getUser()),
                commentCount,
                post.getCreatedAt());
    }
}