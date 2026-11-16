package com.cotato.backend14th.domain.comment.dto;

import java.time.LocalDateTime;

import com.cotato.backend14th.domain.comment.entity.StudyPostComment;
import com.cotato.backend14th.domain.user.dto.AuthorResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentResponse {

    @Schema(description = "댓글 ID", example = "5")
    private Long commentId;

    @Schema(description = "댓글(신청) 내용", example = "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요.")
    private String content;

    @Schema(description = "작성자")
    private AuthorResponse author;

    // boolean 필드의 Lombok getter(isAuthor())는 JSON에서 "author"가 돼 위 author와 겹쳐요. 이름을 직접 지정해요
    @Schema(description = "요청한 사람이 작성자인지 (비로그인이면 false)", example = "false")
    @JsonProperty("isAuthor")
    private boolean isAuthor;

    @Schema(description = "작성 시각", example = "2026-11-08T13:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static CommentResponse of(StudyPostComment comment, boolean isAuthor) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                AuthorResponse.from(comment.getUser()),
                isAuthor,
                comment.getCreatedAt()
        );
    }
}
