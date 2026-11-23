package com.cotato.backend14th.global.common.response;
import java.util.List;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
public class PageResponse<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    // Page는 직접 만든 클래스가 아니라, Spring Data JPA가 기본으로 제공하는 클래스
    public PageResponse(Page<T> page){
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }
}
