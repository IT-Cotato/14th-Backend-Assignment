package com.cotato.backend14th.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

// 단위 테스트에서 쓰는 테스트용 객체 만들기. DB 없이 id를 직접 넣어서 만든다.
public final class Fixtures {

    private Fixtures() {
    }

    public static User user(Long id, String nickname, Part part) {
        User user = User.builder()
                .email(nickname + "@test.com")
                .password("encoded-password")
                .name("테스터")
                .nickname(nickname)
                .generation(14)
                .part(part)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static StudyPost post(Long id, User author, String title) {
        StudyPost post = StudyPost.builder().user(author).title(title).content("내용").build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    public static StudyPostComment comment(Long id, StudyPost post, User author, String content) {
        StudyPostComment comment = StudyPostComment.builder().studyPost(post).user(author).content(content).build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    // 요청 DTO는 JSON으로 채워지도록 setter가 없어서, 필드에 값을 직접 넣어 만든다
    public static <T> T request(Class<T> type, Map<String, Object> fields) {
        T request = BeanUtils.instantiateClass(type);
        fields.forEach((name, value) -> ReflectionTestUtils.setField(request, name, value));
        return request;
    }

    // 던진 예외가 Backend14thException이고 ErrorCode가 기대한 것인지 확인한다
    public static void assertErrorCode(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(Backend14thException.class)
                .extracting(e -> ((Backend14thException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
