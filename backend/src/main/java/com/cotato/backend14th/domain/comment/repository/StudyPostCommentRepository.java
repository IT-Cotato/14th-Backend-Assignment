package com.cotato.backend14th.domain.comment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;

public interface StudyPostCommentRepository extends JpaRepository<StudyPostComment, Long> {

    // 여러 글의 댓글 수를 한 번에 세요. 결과는 [postId, count] 배열이고, 댓글이 없는 글은 결과에 없어요
    @Query("""
            select c.studyPost.id, count(c)
            from StudyPostComment c
            where c.studyPost.id in :postIds
            group by c.studyPost.id
            """)
    List<Object[]> countGroupByPostIds(@Param("postIds") List<Long> postIds);

    // 글 삭제 시 달린 댓글을 하나씩 조회하지 않고 한 번에 지워요
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    void deleteAllByStudyPostId(@Param("postId") Long postId);
}
