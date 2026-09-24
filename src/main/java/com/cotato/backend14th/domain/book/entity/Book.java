// Book.java — 실제 DB 테이블과 매핑되는 객체
package com.cotato.backend14th.domain.book.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor // JPA는 기본 생성자가 필수예요
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 자동으로 id를 증가시켜줘요
    private Long id;

    private String title;
    private String author;
}
