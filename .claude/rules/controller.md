---
paths:
  - "src/main/java/**/controller/**/*.java"
---

# 컨트롤러 작성 규칙

컨트롤러 파일을 읽거나 수정할 때 지켜요.

## 응답
- 반환 타입은 `ResponseEntity<ApiResponse<XxxResponse>>`로 해요.
- 성공 응답은 `ResponseUtils`로 감싸요.
  - 조회, 수정 → `ResponseUtils.ok(data)` (200)
  - 생성 → `ResponseUtils.created(data)` (201)
- `ResponseEntity.ok(...)`를 직접 쓰거나 `ApiResponse`를 직접 `new` 하지 않아요.
- 엔티티를 그대로 반환하지 않아요. 항상 `XxxResponse` DTO로 바꿔서 반환해요.

## 예외
- 실패 상황은 `throw new Backend14thException(ErrorCode.XXX)`로 던져요.
- `RuntimeException`, `IllegalArgumentException` 같은 일반 예외를 직접 던지지 않아요.
- 필요한 ErrorCode가 없으면 `ErrorCode` enum에 `도메인-일련번호` 형식으로 추가해요. (예: `BOOK-002`)
- 컨트롤러 안에서 try-catch로 에러 응답을 직접 만들지 않아요. `GlobalExceptionHandler`가 처리해요.

## Swagger 문서화
- 클래스에 `@Tag(name = "Xxx API", description = "...")`를 붙여요.
- 모든 API 메서드에 `@Operation(summary = "...", description = "...")`를 붙여요.
- `@PathVariable`, `@RequestParam`에는 `@Parameter(description = "...", example = "...")`를 붙여요.

## 기타
- URL은 복수형 명사로 써요. (예: `/books`, `/books/{id}`)
- 의존성은 생성자 주입으로 받아요. (`@Autowired` 필드 주입 금지)

## 예시
```java
@Operation(summary = "책 단건 조회", description = "ID로 책 정보를 조회해요.")
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<BookResponse>> getBook(
        @Parameter(description = "조회할 책의 ID", example = "1")
        @PathVariable Long id) {
    Book book = bookRepository.findById(id)
            .orElseThrow(() -> new Backend14thException(ErrorCode.BOOK_NOT_FOUND));
    return ResponseUtils.ok(new BookResponse(book.getId(), book.getTitle(), book.getAuthor()));
}
```
