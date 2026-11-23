package com.cotato.backend14th.domain.study.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotato.backend14th.domain.study.entity.StudyPost;
import com.cotato.backend14th.domain.study.entity.StudyStatus;
import com.cotato.backend14th.domain.user.entity.Part;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {

    // 작성자 정보가 항상 필요하므로 fetch join으로 한 번에 가져와요
    @Query("select p from StudyPost p join fetch p.user where p.id = :id")
    Optional<StudyPost> findWithUserById(@Param("id") Long id);

    // 값이 null인 조건은 무시해요. 여러 조건을 함께 주면 모두 만족하는 글만 나와요.
    // keyword는 서비스에서 '!'를 이스케이프 문자로 처리해서 넘겨요.
    // 목록 화면에는 작성자가 항상 필요해서 fetch join으로 N+1을 막고, 개수 쿼리는 따로 둬요.
    @Query(value = """
            select p from StudyPost p join fetch p.user u
            where (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
              and (:keyword is null
                   or p.title like concat('%', :keyword, '%') escape '!'
                   or p.content like concat('%', :keyword, '%') escape '!')
            order by p.createdAt desc, p.id desc
            """,
            countQuery = """
            select count(p) from StudyPost p join p.user u
            where (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
              and (:keyword is null
                   or p.title like concat('%', :keyword, '%') escape '!'
                   or p.content like concat('%', :keyword, '%') escape '!')
            """)
    Page<StudyPost> search(
            @Param("keyword") String keyword,
            @Param("status") StudyStatus status,
            @Param("part") Part part,
            Pageable pageable
    );
}
