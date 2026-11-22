package com.cotato.backend14th.domain.studypost.entity;

import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.global.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "study_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_status", nullable = false, length = 20)
    private StudyStatus studyStatus;

    @Builder
    private StudyPost(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
        // 처음 만든 글은 항상 모집 중이에요 (api-spec 5번)
        this.studyStatus = StudyStatus.RECRUITING;
    }

    // PATCH라 보낸 값(null이 아닌 값)만 바꿔요
    public void update(String title, String content, StudyStatus studyStatus) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (studyStatus != null) {
            this.studyStatus = studyStatus;
        }
    }

    public boolean isWrittenBy(Long userId) {
        return userId != null && userId.equals(user.getId());
    }
}
