package com.cotato.backend14th.domain.comment.repository;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.entity.Part;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudyPostCommentRepository extends Repository<StudyPostComment, Long> {

    StudyPostComment save(StudyPostComment comment);

    void delete(StudyPostComment comment);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from StudyPostComment c where c.id = :commentId")
    Optional<StudyPostComment> findByIdForUpdate(Long commentId);

    @Query("""
            select c.studyPost.id as postId, count(c.id) as commentCount
            from StudyPostComment c
            where c.studyPost.id in :postIds
            group by c.studyPost.id
            """)
    List<CommentCount> countByPostIds(Collection<Long> postIds);

    @Modifying
    @Query("delete from StudyPostComment c where c.studyPost.id = :postId")
    void deleteByPostId(Long postId);

    @Query(value = """
            select c.id as commentId, c.content as content, c.createdAt as createdAt,
                   a.id as authorId, a.nickname as nickname, a.generation as generation, a.part as part
            from StudyPostComment c join c.author a
            where c.studyPost.id = :postId
            order by c.createdAt asc, c.id asc
            """, countQuery = "select count(c.id) from StudyPostComment c where c.studyPost.id = :postId")
    Page<CommentRow> findPageByPostId(Long postId, Pageable pageable);

    interface CommentRow {
        Long getCommentId();
        String getContent();
        LocalDateTime getCreatedAt();
        Long getAuthorId();
        String getNickname();
        int getGeneration();
        Part getPart();
    }

    interface CommentCount {
        Long getPostId();
        long getCommentCount();
    }
}
