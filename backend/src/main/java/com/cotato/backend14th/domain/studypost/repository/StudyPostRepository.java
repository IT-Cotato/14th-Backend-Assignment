package com.cotato.backend14th.domain.studypost.repository;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.entity.Part;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {

    @Query(value = """
            select sp.id as postId, sp.title as title, sp.studyStatus as studyStatus, sp.createdAt as createdAt,
                   a.nickname as nickname, a.generation as generation, a.part as part
            """ + FILTERED_POSTS + " order by sp.createdAt desc, sp.id desc",
            countQuery = "select count(sp.id) " + FILTERED_POSTS)
    Page<ListRow> search(String pattern, StudyStatus status, Part part, Pageable pageable);

    @Query("""
            select sp.id as postId, sp.title as title, sp.content as content, sp.studyStatus as studyStatus,
                   sp.createdAt as createdAt, sp.updatedAt as updatedAt,
                   a.id as authorId, a.nickname as nickname, a.generation as generation, a.part as part
            from StudyPost sp join sp.author a
            where sp.id = :postId
            """)
    Optional<DetailRow> findDetailById(Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sp from StudyPost sp where sp.id = :postId")
    Optional<StudyPost> findByIdForUpdate(Long postId);

    interface DetailRow extends ListRow {
        String getContent();
        LocalDateTime getUpdatedAt();
        Long getAuthorId();
    }

    String FILTERED_POSTS = """
            from StudyPost sp join sp.author a
            where (:pattern is null or lower(sp.title) like lower(:pattern) escape '!'
                                   or lower(sp.content) like lower(:pattern) escape '!')
              and (:status is null or sp.studyStatus = :status)
              and (:part is null or a.part = :part)
            """;

    interface ListRow {
        Long getPostId();
        String getTitle();
        StudyStatus getStudyStatus();
        LocalDateTime getCreatedAt();
        String getNickname();
        int getGeneration();
        Part getPart();
    }
}
