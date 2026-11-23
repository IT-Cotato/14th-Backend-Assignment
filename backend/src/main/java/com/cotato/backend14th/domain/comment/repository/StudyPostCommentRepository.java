package com.cotato.backend14th.domain.comment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;

public interface StudyPostCommentRepository extends JpaRepository<StudyPostComment, Long> {

    @Query(
            value = "SELECT c FROM StudyPostComment c JOIN FETCH c.user "
                    + "WHERE c.studyPost.id = :studyPostId ORDER BY c.createdAt ASC",
            countQuery = "SELECT COUNT(c) FROM StudyPostComment c WHERE c.studyPost.id = :studyPostId"
    )
    Page<StudyPostComment> findByStudyPostIdWithUser(@Param("studyPostId") Long studyPostId, Pageable pageable);

    @Query("SELECT c FROM StudyPostComment c JOIN FETCH c.user WHERE c.id = :id")
    Optional<StudyPostComment> findByIdWithUser(@Param("id") Long id);

    void deleteByStudyPostId(Long studyPostId);

    @Query("SELECT c.studyPost.id AS studyPostId, COUNT(c) AS commentCount "
            + "FROM StudyPostComment c WHERE c.studyPost.id IN :studyPostIds GROUP BY c.studyPost.id")
    List<StudyPostCommentCountProjection> countByStudyPostIdIn(@Param("studyPostIds") List<Long> studyPostIds);
}
