---
paths:
  - "src/main/java/**/controller/**"
---

# Controller 작성 규칙

## 필수 어노테이션

- 클래스: `@RestController`, `@RequestMapping("/<도메인>")`, `@Tag`
- 메서드: `@Operation`(summary, description), HTTP 매핑 어노테이션

## 파라미터

- `@PathVariable`에는 반드시 이름을 명시한다.

      @PathVariable("id") Long id

  이름을 생략하면 빌드 환경에 따라 파라미터 이름이 인식되지 않아
  `Name for argument of type [java.lang.Long] not specified` 에러가 난다.

- Swagger 문서에 설명이 나오도록 `@Parameter(description = ..., example = ...)`를 붙인다.

## 응답

- 반환 타입은 `ResponseEntity<ApiResponse<...>>`로 쓴다.
- 응답은 `ResponseUtils.ok(...)` 또는 `ResponseUtils.created(...)`로 감싼다.
- `@ApiResponse`에 적는 상태 코드는 실제로 반환되는 코드와 일치시킨다.

## 예외

- `RuntimeException`을 직접 던지지 않는다.
  `Backend14thException(ErrorCode.XXX)`를 사용한다.
- Controller 안에서 try-catch로 처리하지 않는다. `GlobalExceptionHandler`에 맡긴다.
- 자세한 내용은 `docs/error-handling.md`를 참고한다.

## 하지 말 것

- Controller에 비즈니스 로직을 넣지 않는다.
- Repository를 직접 호출하는 현재 구조는 실습용이다.
  Service 계층이 생기면 Controller는 Service만 호출한다.