package com.cotato.backend14th.domain.comment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;

public interface StudyPostCommentRepository extends JpaRepository<StudyPostComment, Long> {

    // 오래된 순(api-spec 11번). 목록에서 작성자를 바로 쓰니 fetch join으로 N+1을 막아요
    @Query(value = """
            select c from StudyPostComment c join fetch c.user
            where c.studyPost.id = :postId
            order by c.createdAt asc, c.id asc
            """,
            countQuery = "select count(c) from StudyPostComment c where c.studyPost.id = :postId")
    Page<StudyPostComment> findPageByStudyPostId(@Param("postId") Long postId, Pageable pageable);

    // 수정·삭제 전 작성자 확인에 User가 필요해서 함께 가져와요
    @Query("select c from StudyPostComment c join fetch c.user where c.id = :id")
    Optional<StudyPostComment> findWithUserById(@Param("id") Long id);

    // 모집글을 지울 때 댓글을 한 번의 DELETE로 지워요 (하나씩 조회·삭제하지 않으려고)
    // 벌크 연산은 영속성 컨텍스트를 거치지 않아서, 끝난 뒤 컨텍스트를 비워 오래된 상태가 남지 않게 해요
    @Modifying(clearAutomatically = true)
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    int deleteAllByStudyPostId(@Param("postId") Long postId);

    // 목록 한 페이지의 글들 댓글 수를 group by 한 번으로 세요 (글마다 세면 N+1)
    @Query("""
            select c.studyPost.id as postId, count(c) as count
            from StudyPostComment c
            where c.studyPost.id in :postIds
            group by c.studyPost.id
            """)
    List<CommentCount> countByStudyPostIds(@Param("postIds") Collection<Long> postIds);

    interface CommentCount {
        Long getPostId();

        Long getCount();
    }
}
