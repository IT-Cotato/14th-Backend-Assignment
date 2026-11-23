package com.cotato.backend14th.domain.study;

import com.cotato.backend14th.domain.study.dto.CommentDtos.Input;
import com.cotato.backend14th.domain.study.dto.StudyDtos.Update;
import com.cotato.backend14th.domain.study.entity.*;
import com.cotato.backend14th.domain.study.repository.*;
import com.cotato.backend14th.domain.study.service.*;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static com.cotato.backend14th.global.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StudyServicesTest {
    private final StudyPostRepository posts = mock(StudyPostRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final UserRepository users = mock(UserRepository.class);

    @Test
    void aDifferentUserCannotChangeThePost() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        when(posts.findForUpdate(3L)).thenReturn(Optional.of(new StudyPost(owner, "title", "content")));
        var service = new StudyService(posts, users, comments);
        assertThatThrownBy(() -> service.update(3L, 2L, new Update(null, null, StudyStatus.CLOSED)))
                .isInstanceOfSatisfying(Backend14thException.class, error ->
                        org.assertj.core.api.Assertions.assertThat(error.getErrorCode()).isEqualTo(NOT_POST_AUTHOR));
    }

    @Test
    void aClosedPostDoesNotAcceptApplications() {
        StudyPost post = new StudyPost(mock(User.class), "title", "content");
        post.update(null, null, StudyStatus.CLOSED);
        when(posts.findForUpdate(3L)).thenReturn(Optional.of(post));
        var service = new CommentService(posts, comments, users);
        assertThatThrownBy(() -> service.create(3L, 1L, new Input("application")))
                .isInstanceOfSatisfying(Backend14thException.class, error ->
                        org.assertj.core.api.Assertions.assertThat(error.getErrorCode()).isEqualTo(STUDY_POST_CLOSED));
        verifyNoInteractions(comments);
    }
}
