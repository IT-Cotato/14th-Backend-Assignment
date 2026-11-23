package com.cotato.backend14th.domain.studypost.repository;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.entity.Part;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {

    // 목록 검색이에요. 작성자를 fetch join해서 N+1 쿼리를 막아요.
    // 조건이 null이면 그 조건은 적용하지 않아요. 최신순(createdAt, id 내림차순)으로 정렬해요.
    @Query(value = """
            select p from StudyPost p
            join fetch p.user u
            where (:keyword is null
                   or p.title like concat('%', :keyword, '%')
                   or p.content like concat('%', :keyword, '%'))
              and (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
            order by p.createdAt desc, p.id desc
            """,
            countQuery = """
            select count(p) from StudyPost p
            join p.user u
            where (:keyword is null
                   or p.title like concat('%', :keyword, '%')
                   or p.content like concat('%', :keyword, '%'))
              and (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
            """)
    Page<StudyPost> search(
            @Param("keyword") String keyword,
            @Param("status") StudyStatus status,
            @Param("part") Part part,
            Pageable pageable
    );

    // 상세 조회예요. 작성자를 한 번에 가져와요.
    @Query("select p from StudyPost p join fetch p.user where p.id = :postId")
    Optional<StudyPost> findWithUserById(@Param("postId") Long postId);
}
