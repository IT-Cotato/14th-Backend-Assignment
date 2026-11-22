package com.cotato.backend14th.domain.studypost.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.cotato.backend14th.domain.studypost.dto.StudyPostCreateRequest;
import com.cotato.backend14th.domain.studypost.dto.StudyPostDetailResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostIdResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostSummaryResponse;
import com.cotato.backend14th.domain.studypost.dto.StudyPostUpdateRequest;
import com.cotato.backend14th.domain.studypost.entity.StudyPost;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.studypost.repository.StudyPostRepository;
import com.cotato.backend14th.domain.user.entity.Part;
import com.cotato.backend14th.domain.user.entity.User;
import com.cotato.backend14th.domain.user.repository.UserRepository;
import com.cotato.backend14th.global.common.response.PageResponse;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudyPostService {

    private final StudyPostRepository studyPostRepository;
    private final UserRepository userRepository;

    @Transactional
    public StudyPostIdResponse create(Long userId, StudyPostCreateRequest request) {
        // 토큰은 유효하지만 유저가 사라진 경우(DB 초기화 등)는 인증 실패로 봐요
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.UNAUTHORIZED));

        StudyPost post = StudyPost.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return new StudyPostIdResponse(studyPostRepository.save(post).getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<StudyPostSummaryResponse> getList(
            String keyword, StudyStatus status, Part part, int page, int size
    ) {
        // 빈 검색어는 검색 조건이 없는 것으로 봐요
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<StudyPost> posts = studyPostRepository.search(
                normalizedKeyword, status, part, PageRequest.of(page, size));

        // 댓글 기능을 만들면 글 id 목록으로 댓글 수를 한 번에 세서(group by) 채워요. 그 전까지는 0이에요
        return PageResponse.from(posts.map(post -> StudyPostSummaryResponse.of(post, 0L)));
    }

    @Transactional(readOnly = true)
    public StudyPostDetailResponse getDetail(Long postId, Long loginUserId) {
        StudyPost post = studyPostRepository.findWithUserById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));

        // 로그인하지 않았으면 loginUserId가 null이라 false예요
        return StudyPostDetailResponse.of(post, post.isWrittenBy(loginUserId));
    }

    @Transactional
    public StudyPostIdResponse update(Long postId, Long userId, StudyPostUpdateRequest request) {
        StudyPost post = getPostWrittenBy(postId, userId);
        post.update(request.getTitle(), request.getContent(), request.getStudyStatus());
        return new StudyPostIdResponse(post.getId());
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        StudyPost post = getPostWrittenBy(postId, userId);
        // 댓글 기능을 만들면 여기서 글보다 먼저 댓글을 일괄 삭제해요
        // (StudyPostCommentRepository.deleteAllByStudyPostId(postId), 같은 트랜잭션이라 실패하면 함께 롤백돼요)
        studyPostRepository.delete(post);
    }

    // 없는 글이면 404를 먼저 내고, 있을 때만 작성자인지 확인해요 (없는 글의 작성자 여부를 드러내지 않게)
    private StudyPost getPostWrittenBy(Long postId, Long userId) {
        StudyPost post = studyPostRepository.findWithUserById(postId)
                .orElseThrow(() -> new Backend14thException(ErrorCode.STUDY_POST_NOT_FOUND));
        if (!post.isWrittenBy(userId)) {
            throw new Backend14thException(ErrorCode.NOT_POST_AUTHOR);
        }
        return post;
    }
}
