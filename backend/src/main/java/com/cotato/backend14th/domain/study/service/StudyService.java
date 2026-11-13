package com.cotato.backend14th.domain.study.service;

import com.cotato.backend14th.domain.study.dto.StudyDtos.*;
import com.cotato.backend14th.domain.study.entity.*;
import com.cotato.backend14th.domain.study.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.dto.AuthDtos.Author;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cotato.backend14th.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {
    private final StudyPostRepository posts;
    private final UserRepository users;

    @Transactional
    public Id create(long user, Create request) {
        return new Id(posts.save(new StudyPost(users.getReferenceById(user), request.title(), request.content())).getId());
    }

    public PageResponse<Summary> list(String keyword, StudyStatus status, Part part, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var found = posts.search(keyword.strip(), status, part, pageable);
        return PageResponse.from(found.map(post -> new Summary(post.getId(), post.getTitle(), post.getStudyStatus(),
                Author.from(post.getAuthor()), 0, post.getCreatedAt())));
    }

    public Detail detail(long id, Long viewer) {
        return Detail.from(posts.findById(id).orElseThrow(() -> new Backend14thException(STUDY_POST_NOT_FOUND)), viewer);
    }

    @Transactional
    public Id update(long id, long user, Update request) {
        StudyPost post = ownedPost(id, user);
        post.update(request.title(), request.content(), request.studyStatus());
        return new Id(id);
    }

    @Transactional
    public void delete(long id, long user) {
        posts.delete(ownedPost(id, user));
    }

    private StudyPost ownedPost(long id, long user) {
        StudyPost post = posts.findForUpdate(id).orElseThrow(() -> new Backend14thException(STUDY_POST_NOT_FOUND));
        if (!post.getAuthor().getId().equals(user)) throw new Backend14thException(NOT_POST_AUTHOR);
        return post;
    }
}
