package com.cotato.backend14th.domain.study;

import com.cotato.backend14th.domain.user.User;
import com.cotato.backend14th.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "study_post_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyPostComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_post_id", nullable = false)
    private StudyPost studyPost;

    @ManyToOne(fetch = FetchType.LAZY)
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

    public void update(String content) {
        this.content = content;
    }
}
