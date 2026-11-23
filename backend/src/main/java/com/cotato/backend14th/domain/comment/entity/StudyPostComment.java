package com.cotato.backend14th.domain.comment.entity;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 스터디 모집글 댓글이에요. (ERD: StudyPostComment)
// 댓글 작성은 스터디 신청을 의미해요.
@Entity
@Table(name = "study_post_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyPostComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 모집글이 삭제되면 댓글도 함께 삭제돼요.
    // 서비스에서 먼저 일괄 삭제하고, DB FK에도 ON DELETE CASCADE를 걸어 두 번 보장해요.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private StudyPost studyPost;

    // 작성자예요. 목록 조회에서는 fetch join으로 함께 가져와요.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String content;

    public StudyPostComment(StudyPost studyPost, User user, String content) {
        this.studyPost = studyPost;
        this.user = user;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    // 작성자인지 확인해요. user는 프록시여도 id 조회만으로 초기화되지 않아요.
    public boolean isWrittenBy(Long userId) {
        return userId != null && user.getId().equals(userId);
    }
}
