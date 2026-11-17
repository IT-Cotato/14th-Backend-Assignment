package com.cotato.backend14th.domain.study;

import com.cotato.backend14th.domain.user.User;
import com.cotato.backend14th.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "study_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
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
        this.studyStatus = StudyStatus.RECRUITING;
    }

    public void update(String title, String content, StudyStatus studyStatus) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (studyStatus != null) this.studyStatus = studyStatus;
    }
}