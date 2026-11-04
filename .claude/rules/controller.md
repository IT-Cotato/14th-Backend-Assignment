---
paths:
  - "src/main/java/**/controller/**/*.java"
---

# Controller 규칙

- 반환 타입은 `ResponseEntity<ApiResponse<DTO>>`
- 일반 성공은 `ResponseUtils.ok(data)`, 생성은 `ResponseUtils.created(data)`
- 엔티티를 직접 반환하지 않고 DTO로 변환해서 반환
- 요청 바디는 `@RequestBody`로 `XxxRequest` DTO를 받는다
- 데이터가 없는 등의 에러는 `new Backend14thException(ErrorCode.XXX)`로 던진다. Controller에서 에러 응답을 직접 만들지 않는다 (`GlobalExceptionHandler`가 변환)
- 클래스 경로는 `@RequestMapping("/도메인 복수형")` (예: `/books`)
- Swagger 어노테이션을 붙인다
  - 클래스: `@Tag(name, description)`
  - 메서드: `@Operation(summary, description)`
  - 경로 변수: `@Parameter(description, example)`
