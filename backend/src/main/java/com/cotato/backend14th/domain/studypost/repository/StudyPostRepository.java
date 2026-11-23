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
     * 목록 검색. 파라미터가 null이면 그 조건은 건너뛰어요. (keyword, status, part 조합 가능)
     * part는 작성자의 파트라서 User와 조인하고, 목록에서 작성자를 바로 쓰니 fetch join으로 N+1을 막아요.
     * 정렬은 최신순으로 고정하고, 같은 시각이면 id가 큰 글을 먼저 보여줘요.
     */
    @Query(value = """
            select p from StudyPost p join fetch p.user u
            where (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
              and (:keyword is null
                   or p.title like concat('%', :keyword, '%')
                   or p.content like concat('%', :keyword, '%'))
            order by p.createdAt desc, p.id desc
            """,
            countQuery = """
            select count(p) from StudyPost p join p.user u
            where (:status is null or p.studyStatus = :status)
              and (:part is null or u.part = :part)
              and (:keyword is null
                   or p.title like concat('%', :keyword, '%')
                   or p.content like concat('%', :keyword, '%'))
            """)
    Page<StudyPost> search(
            @Param("keyword") String keyword,
            @Param("status") StudyStatus status,
            @Param("part") Part part,
            Pageable pageable
    );

    // 상세 조회는 작성자 정보와 isAuthor 판단에 User가 필요해서 함께 가져와요
    @Query("select p from StudyPost p join fetch p.user where p.id = :id")
    Optional<StudyPost> findWithUserById(@Param("id") Long id);
}
