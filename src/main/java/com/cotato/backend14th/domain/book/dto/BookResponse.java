package com.cotato.backend14th.domain.book.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookResponse {
    private Long id;
    private String title;
    private String author;
}