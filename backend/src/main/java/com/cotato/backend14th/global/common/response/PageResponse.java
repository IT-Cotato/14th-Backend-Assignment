package com.cotato.backend14th.global.common.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

// 목록 API의 페이지 응답이에요. (docs/api-spec.md 3장)
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
