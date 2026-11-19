package com.cotato.backend14th.support;

import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.cotato.backend14th.domain.auth.dto.LoginRequest;
import com.cotato.backend14th.domain.auth.dto.SignupRequest;
import com.cotato.backend14th.domain.comment.dto.CommentRequest;
import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;

// 서비스 단위 테스트에서 쓰는 엔티티와 요청 DTO를 만들어요.
// - 엔티티 id는 원래 DB가 만들어 주는 값이라 setter가 없어서 ReflectionTestUtils로 넣어요.
// - 요청 DTO도 JSON으로만 채워지게 setter가 없어서 같은 방식으로 값을 넣어요.
public final class TestFixtures {

    private TestFixtures() {
    }

    // ---------- 엔티티 ----------

    public static User user(Long id) {
        return user(id, Part.BE);
    }

    public static User user(Long id, Part part) {
        User user = User.builder()
                .email("user" + id + "@test.com")
                .password("encoded-password")
                .name("테스트" + id)
                .nickname("닉네임" + id)
                .generation(14)
                .part(part)
                .build();
        setField(user, "id", id);
        return user;
    }

    public static StudyPost studyPost(Long id, User author) {
        StudyPost post = StudyPost.builder()
                .user(author)
                .title("스터디 모집글 " + id)
                .content("스터디 모집글 내용 " + id)
                .build();
        setField(post, "id", id);
        return post;
    }

    public static StudyPostComment comment(Long id, StudyPost post, User author, String content) {
        StudyPostComment comment = StudyPostComment.builder()
                .studyPost(post)
                .user(author)
                .content(content)
                .build();
        setField(comment, "id", id);
        return comment;
    }

    // ---------- 요청 DTO ----------

    public static SignupRequest signupRequest(
            String email, String password, String name, String nickname, Integer generation, Part part
    ) {
        SignupRequest request = new SignupRequest();
        setField(request, "email", email);
        setField(request, "password", password);
        setField(request, "name", name);
        setField(request, "nickname", nickname);
        setField(request, "generation", generation);
        setField(request, "part", part);
        return request;
    }

    public static LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        setField(request, "email", email);
        setField(request, "password", password);
        return request;
    }

    public static StudyPostCreateRequest studyPostCreateRequest(String title, String content) {
        StudyPostCreateRequest request = new StudyPostCreateRequest();
        setField(request, "title", title);
        setField(request, "content", content);
        return request;
    }

    // 바꾸지 않을 필드는 null로 넘겨요
    public static StudyPostUpdateRequest studyPostUpdateRequest(
            String title, String content, StudyStatus studyStatus
    ) {
        StudyPostUpdateRequest request = new StudyPostUpdateRequest();
        setField(request, "title", title);
        setField(request, "content", content);
        setField(request, "studyStatus", studyStatus);
        return request;
    }

    public static CommentRequest commentRequest(String content) {
        CommentRequest request = new CommentRequest();
        setField(request, "content", content);
        return request;
    }
}
