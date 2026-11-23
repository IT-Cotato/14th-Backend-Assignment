// StudyPostCommentRepository.java
package com.cotato.backend14th.domain.comment.repository;

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

    // 글 하나의 댓글을 작성자와 함께 가져와요
    @Query(
            value = "select c from StudyPostComment c join fetch c.user where c.studyPost.id = :postId",
            countQuery = "select count(c) from StudyPostComment c where c.studyPost.id = :postId"
    )
    Page<StudyPostComment> findPageByStudyPostId(@Param("postId") Long postId, Pageable pageable);

    @Query("select c from StudyPostComment c join fetch c.user where c.id = :commentId")
    Optional<StudyPostComment> findWithUserById(@Param("commentId") Long commentId);

    // 목록 화면의 댓글 수를 글마다 따로 세지 않고 한 번에 가져와요
    @Query("""
            select c.studyPost.id as postId, count(c) as commentCount
            from StudyPostComment c
            where c.studyPost.id in :postIds
            group by c.studyPost.id
            """)
    List<PostCommentCount> countByStudyPostIds(@Param("postIds") List<Long> postIds);

    // 글을 지울 때 달린 댓글을 한 번에 지워요
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    void deleteAllByStudyPostId(@Param("postId") Long postId);

    interface PostCommentCount {
        Long getPostId();

        Long getCommentCount();
    }
}
