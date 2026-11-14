package com.cotato.backend14th.domain.comment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;

public interface StudyPostCommentRepository extends JpaRepository<StudyPostComment, Long> {

    // 모집글별 댓글 수예요. 목록 한 페이지의 글을 한 번에 세서 N+1을 막아요.
    @Query("""
            select c.studyPost.id as postId, count(c) as count
            from StudyPostComment c
            where c.studyPost.id in :postIds
            group by c.studyPost.id
            """)
    List<CommentCount> countByPostIds(@Param("postIds") Collection<Long> postIds);

    // 작성자를 함께 가져오고, 오래된 순으로 정렬해요.
    @Query(value = """
            select c from StudyPostComment c
            join fetch c.user
            where c.studyPost.id = :postId
            order by c.createdAt asc, c.id asc
            """,
            countQuery = "select count(c) from StudyPostComment c where c.studyPost.id = :postId")
    Page<StudyPostComment> findPageByPostId(@Param("postId") Long postId, Pageable pageable);

    // 작성자 확인과 마감 여부 확인에 쓰는 연관 엔티티를 함께 가져와요.
    @Query("select c from StudyPostComment c join fetch c.user where c.id = :commentId")
    Optional<StudyPostComment> findWithUserById(@Param("commentId") Long commentId);

    // 모집글을 지우기 전에 달린 댓글을 한 번에 지워요.
    @Modifying(flushAutomatically = true)
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    int deleteAllByPostId(@Param("postId") Long postId);

    interface CommentCount {
        Long getPostId();

        long getCount();
    }
}
