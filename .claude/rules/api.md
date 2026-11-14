---
paths:
  - "backend/src/main/java/**/*Controller.java"
---

# API 컨트롤러 규칙

## 성공 응답
- 반환 타입은 `ResponseEntity<ApiResponse<T>>`입니다.
- 조회는 `ResponseUtils.ok(data)`, 생성은 `ResponseUtils.created(data)`로 반환합니다.
- 엔티티를 그대로 반환하지 않고 `{도메인}Response`로 변환합니다.

## 예외
- `ErrorCode`에 `{도메인}_{상황}` 상수와 `{도메인}-{3자리 번호}` 코드를 추가합니다.
- `throw new Backend14thException(ErrorCode.상수)`로 던집니다.
- 컨트롤러에서 try-catch로 에러 응답을 만들지 않습니다. `GlobalExceptionHandler`가 처리합니다.

## Swagger
- 컨트롤러에 `@Tag`, 메서드에 `@Operation`, 경로 변수에 `@Parameter`를 붙입니다.

## 기존 예외
- `HelloController`는 `String`을 반환합니다. 사용자 요청 없이 형식을 바꾸지 않습니다.
