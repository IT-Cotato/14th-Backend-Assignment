package com.cotato.backend14th.domain.study.repository;

import com.cotato.backend14th.domain.study.entity.*;
import com.cotato.backend14th.domain.user.entity.Part;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {
    @EntityGraph(attributePaths = "author")
    @Query("""
            select p from StudyPost p join p.author a
            where (:status is null or p.studyStatus = :status)
              and (:part is null or a.part = :part)
              and (locate(lower(:keyword), lower(p.title)) > 0 or locate(lower(:keyword), lower(p.content)) > 0)
            """)
    Page<StudyPost> search(@Param("keyword") String keyword, @Param("status") StudyStatus status,
                           @Param("part") Part part, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from StudyPost p where p.id = :id")
    Optional<StudyPost> findForUpdate(@Param("id") long id);
}
