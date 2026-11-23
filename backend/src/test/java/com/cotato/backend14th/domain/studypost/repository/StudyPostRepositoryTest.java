package com.cotato.backend14th.domain.studypost.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;

/**
 * 검색 쿼리(JPQL)가 조건 조합대로 동작하는지 H2에서 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StudyPostRepositoryTest {

    @Autowired
    private StudyPostRepository studyPostRepository;
    @Autowired
    private UserRepository userRepository;

    private StudyPost algorithmByBe;
    private StudyPost reactByFe;
    private StudyPost closedAlgorithmByFe;

    @BeforeEach
    void setUp() {
        User be = userRepository.save(user("be@gmail.com", Part.BE));
        User fe = userRepository.save(user("fe@gmail.com", Part.FE));
        algorithmByBe = studyPostRepository.save(new StudyPost(be, "알고리즘 스터디", "백준 풀이"));
        reactByFe = studyPostRepository.save(new StudyPost(fe, "리액트 스터디", "알고리즘은 안 해요"));
        closedAlgorithmByFe = new StudyPost(fe, "알고리즘 심화", "100% 출석 _필수_");
        closedAlgorithmByFe.update(null, null, StudyStatus.CLOSED);
        studyPostRepository.save(closedAlgorithmByFe);
    }

    @Test
    void 조건이_없으면_전체를_최신순으로_돌려준다() {
        assertThat(ids(search(null, null, null))).containsExactly(
                closedAlgorithmByFe.getId(), reactByFe.getId(), algorithmByBe.getId());
    }

    @Test
    void 키워드는_제목과_내용에서_찾는다() {
        assertThat(ids(search("%알고리즘%", null, null)))
                .containsExactlyInAnyOrder(algorithmByBe.getId(), reactByFe.getId(), closedAlgorithmByFe.getId());
        assertThat(ids(search("%백준%", null, null))).containsExactly(algorithmByBe.getId());
    }

    @Test
    void 모집_상태와_작성자_파트로_거른다() {
        assertThat(ids(search(null, StudyStatus.CLOSED, null))).containsExactly(closedAlgorithmByFe.getId());
        assertThat(ids(search(null, null, Part.BE))).containsExactly(algorithmByBe.getId());
    }

    @Test
    void 세_조건을_함께_쓰면_모두_만족하는_글만_나온다() {
        assertThat(ids(search("%알고리즘%", StudyStatus.RECRUITING, Part.FE))).containsExactly(reactByFe.getId());
        assertThat(ids(search("%알고리즘%", StudyStatus.CLOSED, Part.BE))).isEmpty();
    }

    @Test
    void 이스케이프한_퍼센트와_밑줄은_글자_그대로_찾는다() {
        assertThat(ids(search("%100!%%", null, null))).containsExactly(closedAlgorithmByFe.getId());
        assertThat(ids(search("%!_필수!_%", null, null))).containsExactly(closedAlgorithmByFe.getId());
        assertThat(ids(search("%!%!%%", null, null))).isEmpty();
    }

    @Test
    void 페이지_크기와_전체_개수를_계산한다() {
        Page<StudyPost> page = studyPostRepository.search(null, null, null, PageRequest.of(1, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(ids(page)).containsExactly(algorithmByBe.getId());
    }

    private Page<StudyPost> search(String keyword, StudyStatus status, Part part) {
        return studyPostRepository.search(keyword, status, part, PageRequest.of(0, 10));
    }

    private List<Long> ids(Page<StudyPost> page) {
        return page.getContent().stream().map(StudyPost::getId).toList();
    }

    private User user(String email, Part part) {
        return User.builder()
                .email(email).password("hashed").name("이름")
                .nickname("닉네임").generation(14).part(part)
                .build();
    }
}
