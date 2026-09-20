package com.cotato.backend14th.domain.book.repository;

import com.cotato.backend14th.domain.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
  
}
