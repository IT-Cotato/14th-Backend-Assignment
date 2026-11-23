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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 모집글 댓글 = 스터디 신청. 글 → 댓글 컬렉션은 두지 않고 댓글 → 글 단방향으로만 연결해요
@Entity
@Table(name = "study_post_comment")
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
