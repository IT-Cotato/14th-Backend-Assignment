package com.cotato.backend14th.domain.study.repository;

import com.cotato.backend14th.domain.study.entity.StudyPostComment;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<StudyPostComment, Long> {
    interface Count {
        Long getPostId();
        long getTotal();
    }

    @EntityGraph(attributePaths = "author")
    Page<StudyPostComment> findByPostId(long postId, Pageable pageable);

    @Query("select c.post.id as postId, count(c) as total from StudyPostComment c where c.post.id in :ids group by c.post.id")
    List<Count> countForPosts(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from StudyPostComment c where c.id = :id")
    Optional<StudyPostComment> findForUpdate(@Param("id") long id);
}
