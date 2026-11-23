package com.cotato.backend14th.domain.comment.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 댓글 목록 (오래된 순). 작성자를 fetch join으로 함께 가져와서 댓글마다 회원 쿼리가 나가지 않게 해요 (N+1 방지)
    @Query(value = """
            select c from StudyPostComment c
            join fetch c.user
            where c.studyPost.id = :postId
            order by c.createdAt asc, c.id asc
            """,
            countQuery = """
            select count(c) from StudyPostComment c
            where c.studyPost.id = :postId
            """)
    Page<StudyPostComment> findAllWithUserByStudyPostId(@Param("postId") Long postId, Pageable pageable);

    // 글 삭제 시 달린 댓글을 하나씩 조회하지 않고 한 번에 지워요
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    void deleteAllByStudyPostId(@Param("postId") Long postId);
}
