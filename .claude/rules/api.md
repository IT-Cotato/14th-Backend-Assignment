---
paths:
  - "src/main/java/**/*.java"
---

# Java API 규칙

- 기능은 `domain/<도메인>` 아래 기존 controller, dto, entity, repository 구조에 맞춘다.
- 도서 성공 응답은 기존 `ResponseUtils`와 `ApiResponse`를 재사용한다.
- 업무 오류는 `ErrorCode`에 정의하고 `Backend14thException`으로 전달한다. Controller마다 예외 응답을 새로 만들지 않는다.
- 영속 엔티티를 API 응답으로 직접 반환하지 않고 응답 DTO로 변환한다.
- 입력 검증 추가 시 DTO 제약, `@Valid`, 전역 예외 처리와 오류 응답까지 함께 확인한다.
- API가 바뀌면 Swagger 설명과 검증 절차도 갱신한다.
