package com.cotato.backend14th.global.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

// 목록 응답의 공통 페이지 형식이에요. 페이지 번호는 0부터 시작해요.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
