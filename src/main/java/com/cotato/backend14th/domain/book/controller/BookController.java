package com.cotato.backend14th.domain.book.controller;

import com.cotato.backend14th.domain.book.dto.BookRequest;
import com.cotato.backend14th.domain.book.dto.BookResponse;
import com.cotato.backend14th.domain.book.entity.Book;
import com.cotato.backend14th.domain.book.repository.BookRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Book API", description = "책 관련 API") // Swagger에서 이 컨트롤러를 "Book API" 그룹으로 묶어줘요
@RestController          // REST API 컨트롤러임을 선언해요
@RequestMapping("/books") // 이 컨트롤러의 모든 API는 /books로 시작해요
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Operation(summary = "책 단건 조회", description = "ID로 책 정보를 조회해요.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "책을 찾을 수 없음") // 가능한 응답 코드를 문서화해요
    })
    @GetMapping("/{id}")  // GET /books/{id} 요청을 처리해요
    public ResponseEntity<BookResponse> getBook(
            @Parameter(description = "조회할 책의 ID", example = "1") // Swagger UI에 파라미터 설명과 예시값이 표시돼요
            @PathVariable Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));
        return ResponseEntity.ok(new BookResponse(book.getId(), book.getTitle(), book.getAuthor()));
    }

    @Operation(summary = "책 등록", description = "새로운 책을 등록해요.")
    @ApiResponse(responseCode = "200", description = "등록 성공")
    @PostMapping           // POST /books 요청을 처리해요
    public ResponseEntity<BookResponse> createBook(@RequestBody BookRequest request) {
        Book savedBook = bookRepository.save(new Book(null, request.getTitle(), request.getAuthor()));
        return ResponseEntity.ok(new BookResponse(savedBook.getId(), savedBook.getTitle(), savedBook.getAuthor()));
    }
}
