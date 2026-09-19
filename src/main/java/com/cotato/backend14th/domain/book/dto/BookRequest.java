// BookRequest.java — 책 등록 시 클라이언트에서 받아오는 데이터
package com.cotato.backend14th.domain.book.dto;
import lombok.Getter;

@Getter
public class BookRequest {
    private String title;
    private String author;
}
