// StudyPostRepository.java
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

    // keyword(제목 또는 내용), status, part(작성자의 파트)는 값이 있을 때만 조건으로 걸어요.
    // 작성자를 함께 가져와서(join fetch) 목록에서 N+1이 생기지 않게 해요
    @Query(
            value = """
                    select p from StudyPost p
                    join fetch p.user u
                    where (:keyword is null
                           or p.title like concat('%', :keyword, '%')
                           or p.content like concat('%', :keyword, '%'))
                      and (:status is null or p.studyStatus = :status)
                      and (:part is null or u.part = :part)
                    """,
            countQuery = """
                    select count(p) from StudyPost p
                    join p.user u
                    where (:keyword is null
                           or p.title like concat('%', :keyword, '%')
                           or p.content like concat('%', :keyword, '%'))
                      and (:status is null or p.studyStatus = :status)
                      and (:part is null or u.part = :part)
                    """
    )
    Page<StudyPost> search(
            @Param("keyword") String keyword,
            @Param("status") StudyStatus status,
            @Param("part") Part part,
            Pageable pageable
    );

    @Query("select p from StudyPost p join fetch p.user where p.id = :postId")
    Optional<StudyPost> findWithUserById(@Param("postId") Long postId);
}
