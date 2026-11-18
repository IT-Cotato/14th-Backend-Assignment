package com.cotato.backend14th.domain.comment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;

public interface StudyPostCommentRepository extends JpaRepository<StudyPostComment, Long> {

    // 글 번호 목록에 대해 글마다 댓글이 몇 개인지 한 번에 센다
    @Query("""
            select c.studyPost.id as postId, count(c) as commentCount
            from StudyPostComment c
            where c.studyPost.id in :postIds
            group by c.studyPost.id
            """)
    List<CommentCountView> countByPostIds(@Param("postIds") List<Long> postIds);

    // 글을 지울 때 달린 댓글을 함께 지운다
    @Modifying
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    void deleteByStudyPostId(@Param("postId") Long postId);

    interface CommentCountView {
        Long getPostId();

        Long getCommentCount();
    }
}