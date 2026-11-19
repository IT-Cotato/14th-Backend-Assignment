---
paths:
  - "backend/src/main/java/**/*.java"
  - "backend/src/test/java/**/*.java"
---

# 백엔드 API 규칙

- 기능은 `backend/src/main/java/com/cotato/backend14th/domain/<도메인>` 아래 controller, dto, entity, repository, service 구조에 맞춘다.
- 모든 API 주소는 `/api`로 시작한다. 명세(`docs/api-spec.md`)의 URL, 필드 이름, 상태 코드를 그대로 지킨다.
- 성공 응답은 `ResponseUtils`로 감싼 `ApiResponse`를 쓴다. 본문이 없는 204는 `ResponseEntity.noContent()`로 감싸지 않고 내려준다.
- 목록 응답은 `PageResponse`(`content`, `page`, `size`, `totalElements`, `totalPages`)를 쓴다.
- 작성자 정보는 `AuthorResponse`(`nickname`, `generation`, `part`)로 내려준다.
- 업무 오류는 `ErrorCode`에 정의하고 `Backend14thException`으로 던진다. 상수 이름은 명세의 이름(`STUDY_POST_NOT_FOUND`), 응답 code는 `도메인-번호`(`STUDY-001`)다.
- 권한: 공개/로그인은 `SecurityConfig`에서, 작성자 본인 확인은 서비스에서 한다. 로그인 사용자는 `@AuthenticationPrincipal Long userId`로 받는다.
- 비밀번호는 `PasswordEncoder`(BCrypt)로 저장하고, 어떤 응답에도 비밀번호를 넣지 않는다.
- 영속 엔티티를 응답으로 직접 반환하지 않고 DTO(record)로 변환한다.
- 연관관계는 모두 `FetchType.LAZY`, Enum은 `EnumType.STRING`이다. 목록 조회에서 작성자는 fetch join으로 가져와 N+1을 피한다.
- 입력 검증은 DTO 제약 + `@Valid`로 하고, 실패 메시지는 사용자에게 그대로 보여줄 문장으로 쓴다.
- 서비스 로직을 바꾸면 `backend/src/test`의 단위 테스트를 함께 고친다.
