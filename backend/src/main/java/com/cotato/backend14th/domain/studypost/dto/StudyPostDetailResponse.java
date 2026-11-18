package com.cotato.backend14th.domain.studypost.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;

import lombok.Getter;

@Getter
public class StudyPostDetailResponse {

    private final Long postId;
    private final String title;
    private final String content;
    private final StudyStatus studyStatus;
    private final AuthorResponse author;
    // JSON 이름이 명세대로 "isAuthor"가 되도록 Boolean(getIsAuthor)로 둔다
    private final Boolean isAuthor;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public StudyPostDetailResponse(StudyPost post, boolean isAuthor) {
        this.postId = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.studyStatus = post.getStudyStatus();
        this.author = AuthorResponse.from(post.getUser());
        this.isAuthor = isAuthor;
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
    }
}