---
description: 컨트롤러/공통 응답 레이어 작업 시 공통 응답 포맷을 지키기 위한 규칙.
paths:
  - "src/main/java/com/cotato/backend14th/**/controller/**"
  - "src/main/java/com/cotato/backend14th/global/common/response/**"
  - "src/main/java/com/cotato/backend14th/global/exception/**"
---

# API 응답 포맷 규칙

- 컨트롤러는 응답을 직접 만들지 말고 `ResponseUtils`를 통해 `ApiResponse<T>`로 감싸서 반환한다.
- 새로운 예외 상황은 `ErrorCode`에 항목을 추가하고, `Backend14thException`을 던지는 방식으로 처리한다. 컨트롤러에서 직접 `ResponseEntity.badRequest()` 등으로 임의 응답을 만들지 않는다.
- `ApiResponse`, `ErrorResponse`의 필드 구조(success/status/data/timestamp 등)는 임의로 변경하지 않는다. 변경이 필요하면 먼저 사용자에게 확인한다.
- 엔티티(`Book` 등)를 컨트롤러 응답으로 직접 반환하지 않고, 반드시 DTO(`BookResponse` 등)로 변환해서 반환한다.
