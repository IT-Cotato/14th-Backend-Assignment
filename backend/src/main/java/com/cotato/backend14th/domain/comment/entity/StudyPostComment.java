package com.cotato.backend14th.domain.comment.entity;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "study_post_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyPostComment extends BaseEntity{

    // PK 이다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Study 하나에 여러 comment가 생길 수 있으므로 ManyToOne이다.'
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_post_id", nullable = false)
    private StudyPost studyPost;

    // 하나의 게시물에 여러 사람이 여러 댓글을 달 수 있으므로 ManyToOne이다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 댓글에 저장할 내용을 content에 담는다.
    @Column(nullable = false, length = 500)
    private String content;

    // 생성자
    @Builder
    public StudyPostComment(StudyPost studyPost, User user, String content){
        this.studyPost = studyPost;
        this.user = user;
        this.content = content;
    }

    public void update(String content){
        this.content = content;
    }

}
