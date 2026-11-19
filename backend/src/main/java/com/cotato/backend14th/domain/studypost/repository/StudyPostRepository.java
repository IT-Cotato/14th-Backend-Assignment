package com.cotato.backend14th.domain.studypost.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.entity.Part;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {

    /** keyword(제목·내용), status, part(작성자 파트)를 AND로 거르고, 작성자를 함께 가져와요 */
    @Query(value = """
            select p from StudyPost p join fetch p.user u
            where (:keyword is null or p.title like concat('%', :keyword, '%')
                                    or p.content like concat('%', :keyword, '%'))
              and (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
            """,
            countQuery = """
            select count(p) from StudyPost p join p.user u
            where (:keyword is null or p.title like concat('%', :keyword, '%')
                                    or p.content like concat('%', :keyword, '%'))
              and (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
            """)
    Page<StudyPost> search(@Param("keyword") String keyword,
                           @Param("status") StudyStatus status,
                           @Param("part") Part part,
                           Pageable pageable);

    @Query("select p from StudyPost p join fetch p.user where p.id = :id")
    Optional<StudyPost> findWithUserById(@Param("id") Long id);
}
