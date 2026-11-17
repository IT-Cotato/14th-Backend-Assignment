package com.cotato.backend14th.domain.study;

import com.cotato.backend14th.domain.user.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {

    @EntityGraph(attributePaths = "user")
    @Query("""
            select p from StudyPost p
            join p.user u
            where (:keyword is null or :keyword = ''
                or p.title like concat('%', :keyword, '%')
                or p.content like concat('%', :keyword, '%'))
              and (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
            """)
    Page<StudyPost> search(
            @Param("keyword") String keyword,
            @Param("status") StudyStatus status,
            @Param("part") Part part,
            Pageable pageable);
}
