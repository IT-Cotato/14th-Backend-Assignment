package com.cotato.backend14th.domain.study;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyPostCommentRepository extends JpaRepository<StudyPostComment, Long> {

    @EntityGraph(attributePaths = "user")
    Page<StudyPostComment> findByStudyPostIdOrderByCreatedAtAsc(Long studyPostId, Pageable pageable);

    long countByStudyPostId(Long studyPostId);
}
