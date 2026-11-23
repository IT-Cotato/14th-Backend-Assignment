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

    @Query(value = """
            select c from StudyPostComment c
            join fetch c.user
            where c.studyPost.id = :postId
            order by c.createdAt asc, c.id asc
            """,
            countQuery = "select count(c) from StudyPostComment c where c.studyPost.id = :postId")
    Page<StudyPostComment> findByPostId(@Param("postId") Long postId, Pageable pageable);

    @Query("select c from StudyPostComment c join fetch c.user where c.id = :id")
    Optional<StudyPostComment> findWithUserById(@Param("id") Long id);

    /**
     * 목록 화면의 댓글 수를 글마다 따로 세지 않고 한 번에 가져와요.
     */
    @Query("""
            select c.studyPost.id as postId, count(c) as commentCount
            from StudyPostComment c
            where c.studyPost.id in :postIds
            group by c.studyPost.id
            """)
    List<CommentCount> countByPostIds(@Param("postIds") Collection<Long> postIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    int deleteAllByPostId(@Param("postId") Long postId);

    interface CommentCount {
        Long getPostId();

        Long getCommentCount();
    }
}
