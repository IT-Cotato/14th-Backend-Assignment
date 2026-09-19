// BookRepository.java — DB 접근을 담당해요
package com.cotato.backend14th.domain.book.repository;
import com.cotato.backend14th.domain.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BookRepository extends JpaRepository<Book, Long> {
    // save(), findById() 등 기본 CRUD 메서드는 JpaRepository가 자동으로 제공
}
