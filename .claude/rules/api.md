---
paths:
  - "backend/src/main/java/**/*Controller.java"
---

# API 컨트롤러 규칙

## 기준 문서
- 주소, 권한, 요청/응답 필드, 에러 코드는 `docs/api-spec.md`를 따릅니다.
- 주소는 `/api`로 시작합니다. 3주차 예제(`/hello`, `/books`)만 예외입니다.

## 계층
- 컨트롤러는 service만 호출합니다. repository를 직접 호출하지 않습니다. (3주차 `BookController`는 예외)
- 권한 검사(작성자 확인), 비즈니스 규칙, 트랜잭션은 service에 둡니다.

## 성공 응답
- 반환 타입은 `ResponseEntity<ApiResponse<T>>`입니다.
- 조회와 수정은 `ResponseUtils.ok(data)`, 생성은 `ResponseUtils.created(data)`로 반환합니다.
- 본문이 없는 204는 `ResponseEntity<Void>`로 선언하고 `ResponseEntity.noContent().build()`로 반환합니다. 감싸지 않습니다.
- 엔티티를 그대로 반환하지 않고 `{도메인}Response`로 변환합니다.

## 요청 검증
- 요청 DTO에 검증 어노테이션을 붙이고 컨트롤러 파라미터에 `@Valid`를 붙입니다.
- 검증 실패는 `INVALID_INPUT`(400)입니다. `COMMON_BAD_REQUEST`는 쓰지 않습니다.
- 쿼리 파라미터는 `@RequestParam`에 `@Min`, `@Max`를 직접 붙입니다. 컨트롤러에 `@Validated`를 붙이지 않습니다. (Spring 6.1+는 MVC가 직접 검증해서 `HandlerMethodValidationException`을 던지고, `GlobalExceptionHandler`가 400으로 처리합니다. `@Validated`를 붙이면 AOP 검증이 겹치고 처리하지 않는 `ConstraintViolationException`이 나서 500이 될 수 있습니다)
- Enum 쿼리 파라미터(`StudyStatus status` 등)에 없는 값이 오면 `MethodArgumentTypeMismatchException` → 400입니다.

## 로그인 사용자
- 사용자 ID는 `@Parameter(hidden = true) @AuthenticationPrincipal Long userId`로 받습니다.
- 공개 API에서도 유효한 토큰이 있으면 userId가 들어오고, 없으면 null입니다. (`isAuthor` 판단에 사용)
- `isAuthor` 같은 boolean 필드는 `@JsonProperty("isAuthor")`를 붙여 이름을 고정합니다.

## 예외
- 상수 이름과 응답 code는 `docs/api-spec.md` 8장의 값을 그대로 씁니다.
- 8장에 없는 에러를 새로 만들 때만 `{도메인}_{상황}` 상수와 `{도메인}-{3자리 번호}` 코드를 쓰고, 추가 전에 사용자와 정합니다.
- `throw new Backend14thException(ErrorCode.상수)`로 던집니다.
- 컨트롤러에서 try-catch로 에러 응답을 만들지 않습니다. `GlobalExceptionHandler`가 처리합니다.

## Swagger
- 컨트롤러에 `@Tag`, 메서드에 `@Operation`, 경로 변수에 `@Parameter`를 붙입니다.

## 기존 예외
- `HelloController`는 `String`을 반환합니다. 사용자 요청 없이 형식을 바꾸지 않습니다.
