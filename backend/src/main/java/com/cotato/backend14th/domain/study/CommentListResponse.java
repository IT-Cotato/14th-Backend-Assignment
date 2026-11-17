package com.cotato.backend14th.domain.study;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentListResponse {
    private List<CommentItemResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
