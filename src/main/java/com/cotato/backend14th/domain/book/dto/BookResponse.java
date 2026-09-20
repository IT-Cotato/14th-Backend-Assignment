package com.cotato.backend14th.domain.book.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// BookResponse.java — 클라이언트에게 반환할 책 데이터
@Getter
@AllArgsConstructor
public class BookResponse {

  private Long id;
  private String title;
  private String author;
}
