package com.cotato.backend14th.domain.comment.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;

public interface CommentRepository extends JpaRepository<StudyPostComment, Long> {

    // 한 모집글의 댓글 목록. 오래된 순이에요.
    // 작성자를 fetch join으로 함께 가져와서 댓글마다 작성자 조회 쿼리가 나가지 않게 해요 (N+1 방지)
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
    Page<StudyPostComment> findPageByPostId(@Param("postId") Long postId, Pageable pageable);

    // 여러 모집글의 댓글 수를 쿼리 한 번으로 세요. 각 행은 [postId, 댓글 수]예요.
    // 댓글이 없는 글은 결과에 나오지 않아요.
    @Query("""
            select c.studyPost.id, count(c) from StudyPostComment c
            where c.studyPost.id in :postIds
            group by c.studyPost.id
            """)
    List<Object[]> countByPostIds(@Param("postIds") Collection<Long> postIds);

    // 모집글을 지울 때 달린 댓글을 한 번에 지워요 (벌크 delete)
    @Modifying
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
