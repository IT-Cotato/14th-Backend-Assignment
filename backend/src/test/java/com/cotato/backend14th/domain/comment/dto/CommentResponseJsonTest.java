package com.cotato.backend14th.domain.comment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.cotato.backend14th.domain.user.entity.Part;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// isAuthor 이름 충돌과 날짜 형식은 명세와 바로 어긋나기 쉬워서 JSON으로 직접 확인해요
class CommentResponseJsonTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("isAuthor는 \"isAuthor\" 키로, author는 객체로, 날짜는 yyyy-MM-ddTHH:mm:ss로 나가요")
    void serialize() {
        CommentResponse response = new CommentResponse(
                5L, "참여하고 싶어요", new AuthorResponse("철수", 13, Part.FE),
                false, LocalDateTime.of(2026, 11, 8, 13, 0, 0, 987_000_000));

        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(response));

        assertThat(json.get("commentId").asLong()).isEqualTo(5L);
        assertThat(json.has("isAuthor")).isTrue();
        assertThat(json.get("isAuthor").asBoolean()).isFalse();
        assertThat(json.get("author").isObject()).isTrue();
        assertThat(json.get("author").get("part").asString()).isEqualTo("FE");
        assertThat(json.get("createdAt").asString()).isEqualTo("2026-11-08T13:00:00");
    }
}
