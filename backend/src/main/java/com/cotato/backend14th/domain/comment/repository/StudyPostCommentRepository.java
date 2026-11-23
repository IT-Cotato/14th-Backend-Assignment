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

    // 모집글 하나의 댓글 수예요. 목록에서 글마다 따로 세지 않고 한 번에 세기 위해 사용해요.
    interface PostCommentCount {
        Long getPostId();

        long getCommentCount();
    }

    // 작성자 정보가 항상 필요하므로 fetch join으로 한 번에 가져와요
    @Query("select c from StudyPostComment c join fetch c.user where c.id = :id")
    Optional<StudyPostComment> findWithUserById(@Param("id") Long id);

    // 오래된 순이에요. 같은 시각이면 ID 순으로 정렬해서 순서가 흔들리지 않게 해요.
    @Query(value = """
            select c from StudyPostComment c join fetch c.user
            where c.studyPost.id = :postId
            order by c.createdAt asc, c.id asc
            """,
            countQuery = "select count(c) from StudyPostComment c where c.studyPost.id = :postId")
    Page<StudyPostComment> findByPostId(@Param("postId") Long postId, Pageable pageable);

    // 글 여러 개의 댓글 수를 쿼리 한 번으로 세요. (댓글이 없는 글은 결과에 나오지 않아요)
    @Query("""
            select c.studyPost.id as postId, count(c) as commentCount
            from StudyPostComment c
            where c.studyPost.id in :postIds
            group by c.studyPost.id
            """)
    List<PostCommentCount> countByPostIds(@Param("postIds") Collection<Long> postIds);

    // 모집글을 삭제할 때 댓글을 쿼리 한 번으로 지워요. 엔티티를 불러오지 않고 SQL을 바로 실행해요.
    @Modifying
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    int deleteByStudyPostId(@Param("postId") Long postId);
}
