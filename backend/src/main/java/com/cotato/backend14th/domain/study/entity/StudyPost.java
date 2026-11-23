package com.cotato.backend14th.domain.study.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_status", nullable = false)
    private StudyStatus studyStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private StudyPost(String title, String content, StudyStatus studyStatus, User user) {
        this.title = title;
        this.content = content;
        this.studyStatus = studyStatus;
        this.user = user;
    }

    // 처음 만들면 모집 중(RECRUITING) 상태예요
    public static StudyPost create(String title, String content, User user) {
        return new StudyPost(title, content, StudyStatus.RECRUITING, user);
    }

    // null인 값은 바꾸지 않아요 (보낸 필드만 수정)
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

    public boolean isAuthor(Long userId) {
        return userId != null && userId.equals(user.getId());
    }
}
