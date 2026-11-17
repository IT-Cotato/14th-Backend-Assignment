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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "study_post")
public class StudyPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관관계는 모두 LAZY로 해요. 작성자 정보가 필요한 조회는 fetch join으로 함께 가져와요.
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

    private StudyPost(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.studyStatus = StudyStatus.RECRUITING; // 처음 만들면 항상 모집 중이에요
    }

    public static StudyPost create(User user, String title, String content) {
        return new StudyPost(user, title, content);
    }

    // 바꿀 필드만 넘겨요. null인 필드는 그대로 둬요.
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
        return userId != null && user.getId().equals(userId);
    }
}
