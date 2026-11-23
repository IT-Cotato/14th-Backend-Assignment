package com.cotato.backend14th.domain.comment.entity;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.global.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 댓글이 곧 스터디 신청이에요. 연관관계는 모두 단방향이고 cascade는 쓰지 않아요.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyPostComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_post_id", nullable = false)
    private StudyPost studyPost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String content;

    @Builder
    private StudyPostComment(StudyPost studyPost, User user, String content) {
        this.studyPost = studyPost;
        this.user = user;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isWrittenBy(Long userId) {
        return userId != null && userId.equals(user.getId());
    }
}
