package com.cotato.backend14th.global.common.response;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 목록 API 공통 페이지 응답이에요 (모집글 목록, 댓글 목록)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResponse<T> {

    @Schema(description = "목록")
    private final List<T> content;

    @Schema(description = "페이지 번호 (0부터)", example = "0")
    private final int page;

    @Schema(description = "페이지 크기", example = "10")
    private final int size;

    @Schema(description = "전체 개수", example = "1")
    private final long totalElements;

    @Schema(description = "전체 페이지 수", example = "1")
    private final int totalPages;

    public static <T> PageResponse<T> from(Page<T> page) {
        return of(page, page.getContent());
    }

    // 엔티티 Page의 페이지 정보에 DTO 목록을 담을 때 써요
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
