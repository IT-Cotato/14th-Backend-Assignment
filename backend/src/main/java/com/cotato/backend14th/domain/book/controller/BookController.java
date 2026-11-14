package com.cotato.backend14th.domain.book.controller;

import com.cotato.backend14th.domain.book.dto.BookRequest;
import com.cotato.backend14th.domain.book.dto.BookResponse;
import com.cotato.backend14th.domain.book.entity.Book;
import com.cotato.backend14th.domain.book.repository.BookRepository;
import com.cotato.backend14th.global.exception.Backend14thException;
import com.cotato.backend14th.global.exception.ErrorCode;
import com.cotato.backend14th.global.common.response.ApiResponse;
import com.cotato.backend14th.global.common.response.ResponseUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Book API", description = "책 관련 API")
@RestController
@RequestMapping("/books")
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Operation(summary = "책 단건 조회", description = "ID로 책 정보를 조회해요.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponse>> getBook(
            @Parameter(description = "조회할 책의 ID", example = "1")
            @PathVariable Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new Backend14thException(ErrorCode.BOOK_NOT_FOUND));
        return ResponseUtils.ok(new BookResponse(book.getId(), book.getTitle(), book.getAuthor()));
    }

    @Operation(summary = "책 등록", description = "새로운 책을 등록해요.")
    @PostMapping
    public ResponseEntity<ApiResponse<BookResponse>> createBook(@RequestBody BookRequest request) {
        Book savedBook = bookRepository.save(new Book(null, request.getTitle(), request.getAuthor()));
        return ResponseUtils.created(new BookResponse(savedBook.getId(), savedBook.getTitle(), savedBook.getAuthor()));
    }
}