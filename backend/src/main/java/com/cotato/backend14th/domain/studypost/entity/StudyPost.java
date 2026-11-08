package com.cotato.backend14th.domain.studypost.entity;

import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// 스터디 모집글이에요. (ERD: StudyPost)
// 댓글(StudyPostComment)은 댓글 쪽에서 study_post_id로 이 글을 참조할 예정이에요.
@Entity
@Table(name = "study_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자예요. 목록·상세 조회에서는 fetch join으로 함께 가져와요.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    // Hibernate 6은 MySQL에서 Enum을 ENUM 타입으로 만들기 때문에 ERD대로 VARCHAR(20)을 지정해요.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "study_status", nullable = false, length = 20)
    private StudyStatus studyStatus;

    // 새 모집글은 항상 RECRUITING 상태로 시작해요.
    public StudyPost(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.studyStatus = StudyStatus.RECRUITING;
    }

    // PATCH 수정이에요. null인 필드는 바꾸지 않아요.
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

    // 작성자인지 확인해요. user는 프록시여도 id 조회만으로 초기화되지 않아요.
    public boolean isWrittenBy(Long userId) {
        return userId != null && user.getId().equals(userId);
    }
}
