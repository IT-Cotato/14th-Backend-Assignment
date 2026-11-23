package com.cotato.backend14th.domain.comment.repository;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StudyPostCommentRepository extends JpaRepository<StudyPostComment, Long> {

    // 모집글의 댓글 목록이에요. 작성자를 fetch join해서 N+1 쿼리를 막고, 오래된 순으로 정렬해요.
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
    Page<StudyPostComment> findPageByStudyPostId(@Param("postId") Long postId, Pageable pageable);

    // 여러 모집글의 댓글 수를 쿼리 한 번으로 세요. 댓글이 없는 글은 결과에 없어요.
    @Query("""
            select c.studyPost.id as postId, count(c) as commentCount
            from StudyPostComment c
            where c.studyPost.id in :postIds
            group by c.studyPost.id
            """)
    List<PostCommentCount> countByStudyPostIds(@Param("postIds") Collection<Long> postIds);

    // 모집글을 삭제하기 전에 그 글의 댓글을 한 번에 지워요.
    @Modifying(flushAutomatically = true)
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    int deleteAllByStudyPostId(@Param("postId") Long postId);

    long countByStudyPostId(Long postId);

    interface PostCommentCount {
        Long getPostId();

        Long getCommentCount();
    }
}
