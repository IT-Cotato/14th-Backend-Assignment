package com.cotato.backend14th.domain.studypost.service;

import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository;
import com.cotato.backend14th.domain.comment.repository.StudyPostCommentRepository.CommentCount;
import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostPageResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostPageResponse.Author;
import com.cotato.backend14th.domain.studypost.dto.StudyPostPageResponse.Item;
import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateResponse;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository.DetailRow;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository.ListRow;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.service.CurrentUserService;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyPostService {

    private final CurrentUserService currentUserService;
    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository commentRepository;

    @Transactional(readOnly = true)
    public StudyPostPageResponse list(String keyword, StudyStatus status, Part part, Pageable pageable) {
        String pattern = keyword == null || keyword.isEmpty()
                ? null : "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Page<ListRow> page = studyPostRepository.search(pattern, status, part, pageable);
        List<Long> postIds = page.getContent().stream().map(ListRow::getPostId).toList();
        Map<Long, Long> counts = postIds.isEmpty() ? Map.of()
                : commentRepository.countByPostIds(postIds).stream()
                        .collect(Collectors.toMap(CommentCount::getPostId, CommentCount::getCommentCount));

        List<Item> items = page.getContent().stream()
                .map(row -> new Item(row.getPostId(), row.getTitle(), row.getStudyStatus(),
                        new Author(row.getNickname(), row.getGeneration(), row.getPart()),
                        counts.getOrDefault(row.getPostId(), 0L), row.getCreatedAt()))
                .toList();
        return new StudyPostPageResponse(items, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public StudyPostDetailResponse detail(Long postId, Jwt viewer) {
        DetailRow post = studyPostRepository.findDetailById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
        boolean isAuthor = viewer != null && post.getAuthorId().toString().equals(viewer.getSubject());
        return new StudyPostDetailResponse(post.getPostId(), post.getTitle(), post.getContent(), post.getStudyStatus(),
                new Author(post.getNickname(), post.getGeneration(), post.getPart()), isAuthor,
                post.getCreatedAt(), post.getUpdatedAt());
    }

    @Transactional
    public StudyPostUpdateResponse update(Long postId, Jwt jwt, StudyPostUpdateRequest request) {
        StudyPost post = requireOwnedPostForUpdate(postId, jwt);
        post.update(request.getTitle(), request.getContent(), request.getStudyStatus());
        return new StudyPostUpdateResponse(post.getId());
    }

    @Transactional
    public void delete(Long postId, Jwt jwt) {
        StudyPost post = requireOwnedPostForUpdate(postId, jwt);
        commentRepository.deleteByPostId(postId);
        studyPostRepository.delete(post);
    }

    private StudyPost requireOwnedPostForUpdate(Long postId, Jwt jwt) {
        User user = currentUserService.requireUser(jwt);
        StudyPost post = studyPostRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new Backend14thException(ErrorCode.NOT_POST_AUTHOR);
        }
        return post;
    }

    @Transactional
    public StudyPostCreateResponse create(Jwt jwt, StudyPostCreateRequest request) {
        User author = currentUserService.requireUser(jwt);
        StudyPost post = studyPostRepository.save(new StudyPost(request.title(), request.content(), author));
        return new StudyPostCreateResponse(post.getId());
    }
}
