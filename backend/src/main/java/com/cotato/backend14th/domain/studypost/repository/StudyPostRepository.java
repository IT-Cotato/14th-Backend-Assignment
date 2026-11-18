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

    // 키워드, 모집 상태, 작성자 파트를 모두 선택 조건으로 쓴다. (값이 null이면 그 조건은 무시)
    // 파트는 작성자(User)의 값이라서 StudyPost와 User를 조인한다.
    @Query(value = """
            select p from StudyPost p join fetch p.user u
            where (:keyword is null or p.title like concat('%', :keyword, '%')
                                    or p.content like concat('%', :keyword, '%'))
              and (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
            order by p.createdAt desc, p.id desc
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

    // 상세 조회: 작성자 정보까지 한 번에 가져온다
    @Query("select p from StudyPost p join fetch p.user where p.id = :id")
    Optional<StudyPost> findWithUserById(@Param("id") Long id);
}