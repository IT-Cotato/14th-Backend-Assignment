---
paths:
  - "src/main/java/**/controller/**"
---

# Controller 작성 규칙

- 클래스에 `@Tag(name = "도메인 API", description = "설명")` Swagger 어노테이션을 반드시 붙인다.
- 각 API 메서드에 `@Operation(summary, description)` 어노테이션을 붙인다.
- 응답은 `ResponseUtils.ok()` 또는 `ResponseUtils.created()`로 감싼다. `ResponseEntity.ok()`를 직접 쓰지 않는다.
- 예외는 `throw new Backend14thException(ErrorCode.XXX)`로 던진다. Controller에서 try-catch 하지 않는다.
- Controller에 비즈니스 로직을 넣지 않는다. Service를 호출하고 결과만 반환한다.
