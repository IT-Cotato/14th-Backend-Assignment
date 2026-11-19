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

    @Query(value = "select c from StudyPostComment c join fetch c.user where c.studyPost.id = :postId",
            countQuery = "select count(c) from StudyPostComment c where c.studyPost.id = :postId")
    Page<StudyPostComment> findByPostId(@Param("postId") Long postId, Pageable pageable);

    @Query("select c from StudyPostComment c join fetch c.user where c.id = :id")
    Optional<StudyPostComment> findWithUserById(@Param("id") Long id);

    /** 목록의 commentCount를 한 번에 세요 (N+1 방지). [postId, count] */
    @Query("select c.studyPost.id, count(c) from StudyPostComment c where c.studyPost.id in :postIds group by c.studyPost.id")
    List<Object[]> countByPostIds(@Param("postIds") Collection<Long> postIds);

    @Modifying(clearAutomatically = true)
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
