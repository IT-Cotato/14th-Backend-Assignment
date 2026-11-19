package com.cotato.backend14th.support;

import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

/** 단위 테스트용 엔티티. id는 DB 없이 리플렉션으로 넣어요. */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user(Long id, Part part) {
        User user = User.builder()
                .email("user" + id + "@test.com")
                .password("encoded")
                .name("이름" + id)
                .nickname("닉네임" + id)
                .generation(14)
                .part(part)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static StudyPost post(Long id, User author) {
        StudyPost post = StudyPost.builder().user(author).title("제목" + id).content("내용" + id).build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    public static StudyPostComment comment(Long id, StudyPost post, User author) {
        StudyPostComment comment = StudyPostComment.builder().studyPost(post).user(author).content("댓글" + id).build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
