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

    /**
     * 키워드(제목, 내용), 모집 상태, 작성자 파트로 거르고 최신순으로 정렬해요.
     * 조건 값이 null이면 그 조건은 적용하지 않아요. keyword는 '%키워드%' 형태로 넘겨요.
     */
    @Query(value = """
            select p from StudyPost p
            join fetch p.user u
            where (:keyword is null or p.title like :keyword escape '!' or p.content like :keyword escape '!')
              and (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
            order by p.createdAt desc, p.id desc
            """,
            countQuery = """
            select count(p) from StudyPost p
            join p.user u
            where (:keyword is null or p.title like :keyword escape '!' or p.content like :keyword escape '!')
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
