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

    @Query(
            value = "SELECT sp FROM StudyPost sp JOIN FETCH sp.user u "
                    + "WHERE (:keyword IS NULL OR sp.title LIKE CONCAT('%', :keyword, '%') "
                    + "    OR sp.content LIKE CONCAT('%', :keyword, '%')) "
                    + "AND (:status IS NULL OR sp.studyStatus = :status) "
                    + "AND (:part IS NULL OR u.part = :part) "
                    + "ORDER BY sp.createdAt DESC",
            countQuery = "SELECT COUNT(sp) FROM StudyPost sp JOIN sp.user u "
                    + "WHERE (:keyword IS NULL OR sp.title LIKE CONCAT('%', :keyword, '%') "
                    + "    OR sp.content LIKE CONCAT('%', :keyword, '%')) "
                    + "AND (:status IS NULL OR sp.studyStatus = :status) "
                    + "AND (:part IS NULL OR u.part = :part)"
    )
    Page<StudyPost> search(
            @Param("keyword") String keyword,
            @Param("status") StudyStatus status,
            @Param("part") Part part,
            Pageable pageable
    );

    @Query("SELECT sp FROM StudyPost sp JOIN FETCH sp.user WHERE sp.id = :id")
    Optional<StudyPost> findByIdWithUser(@Param("id") Long id);
}
