package com.cotato.backend14th.domain.studypost.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.entity.Part;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {

    // 모집글 목록 검색
    // - 조건 값이 null이면 그 조건은 무시해요 (keyword, status, part 모두 선택)
    // - part는 작성자의 파트라서 users와 조인해요
    // - 작성자를 fetch join으로 함께 가져와서 글마다 작성자 조회 쿼리가 나가지 않게 해요 (N+1 방지)
    // - 최신순. createdAt이 같으면 id가 큰(나중에 만든) 글이 먼저 나와요
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
}
