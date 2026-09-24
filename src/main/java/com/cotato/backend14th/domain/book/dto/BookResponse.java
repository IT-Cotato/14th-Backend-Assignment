// BookResponse.java — 클라이언트에게 반환할 책 데이터
package com.cotato.backend14th.domain.book.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 모든 필드를 받는 생성자를 자동으로 만들어줘요
public class BookResponse {
    private Long id;
    private String title;
    private String author;
}