package com.cotato.backend14th.domain.studypost.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cotato.backend14th.domain.comment.dto.CommentResponse;
import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.cotato.backend14th.domain.user.entity.Part;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// isAuthor(boolean)와 author(객체)가 JSON에서 서로 덮어쓰지 않는지, 날짜 형식이 명세와 같은지 확인해요
class StudyPostResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final AuthorResponse author = new AuthorResponse("길동이", 14, Part.BE);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 11, 8, 12, 0, 0, 123_000_000);

    @Test
    @DisplayName("모집글 상세 응답은 author와 isAuthor를 함께 내려주고 날짜는 초 단위까지만 내려준다")
    void detailResponse() throws Exception {
        StudyPostDetailResponse response = new StudyPostDetailResponse(
                10L, "제목", "내용", StudyStatus.RECRUITING, author, true, createdAt, createdAt);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("isAuthor").asBoolean()).isTrue();
        assertThat(json.get("author").get("nickname").asText()).isEqualTo("길동이");
        assertThat(json.get("author").get("part").asText()).isEqualTo("BE");
        assertThat(json.get("studyStatus").asText()).isEqualTo("RECRUITING");
        assertThat(json.get("createdAt").asText()).isEqualTo("2026-11-08T12:00:00");
        assertThat(json.get("updatedAt").asText()).isEqualTo("2026-11-08T12:00:00");
        assertThat(json.size()).isEqualTo(8);
    }

    @Test
    @DisplayName("댓글 응답은 author와 isAuthor를 함께 내려준다")
    void commentResponse() throws Exception {
        CommentResponse response = new CommentResponse(5L, "신청합니다", author, false, createdAt);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("isAuthor").asBoolean()).isFalse();
        assertThat(json.get("author").get("generation").asInt()).isEqualTo(14);
        assertThat(json.get("createdAt").asText()).isEqualTo("2026-11-08T12:00:00");
        assertThat(json.size()).isEqualTo(5);
    }
}
