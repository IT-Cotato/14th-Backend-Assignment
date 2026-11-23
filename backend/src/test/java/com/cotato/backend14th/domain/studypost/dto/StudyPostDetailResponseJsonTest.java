package com.cotato.backend14th.domain.studypost.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cotato.backend14th.domain.studypost.entity.StudyStatus;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.cotato.backend14th.domain.user.entity.Part;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// isAuthor 이름 충돌과 날짜 형식은 명세와 바로 어긋나기 쉬워서 JSON으로 직접 확인해요
class StudyPostDetailResponseJsonTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("isAuthor는 \"isAuthor\" 키로, author는 객체로, 날짜는 yyyy-MM-ddTHH:mm:ss로 나가요")
    void serialize() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 8, 12, 0, 0, 123_456_000);
        StudyPostDetailResponse response = new StudyPostDetailResponse(
                10L, "알고리즘 스터디 모집", "주 2회 백준 풀이", StudyStatus.RECRUITING,
                new AuthorResponse("길동이", 14, Part.BE),
                true, createdAt, createdAt);

        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(response));

        assertThat(json.get("isAuthor").asBoolean()).isTrue();
        assertThat(json.get("author").isObject()).isTrue();
        assertThat(json.get("author").get("nickname").asString()).isEqualTo("길동이");
        assertThat(json.get("createdAt").asString()).isEqualTo("2026-11-08T12:00:00");
        assertThat(json.get("updatedAt").asString()).isEqualTo("2026-11-08T12:00:00");
        assertThat(json.get("studyStatus").asString()).isEqualTo("RECRUITING");
    }
}
