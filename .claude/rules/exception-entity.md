---
paths:
  - "backend/src/main/java/**/exception/**"
  - "backend/src/main/java/**/entity/**"
---

# 예외 / 엔티티 규칙

## ErrorCode
- 새 에러는 `global/exception/ErrorCode`에 추가한다. 도메인별 예외 enum을 따로 만들지 않는다.
- 형식: `<DOMAIN>_<상황>(HttpStatus.X, "<DOMAIN>-<3자리 번호>", "<한글 메시지>.")`
- 도메인 그룹 주석(`// STUDY`) 아래에 모으고, 번호는 그룹 안에서 이어 붙인다. 기존 코드 번호는 바꾸지 않는다.
- 마지막 상수 뒤의 `;` 위치가 깨지지 않게 주의한다.
- 5,6주차 과제 명세에 상수 이름이 정해진 에러(`INVALID_INPUT`, `UNAUTHORIZED`, `NOT_POST_AUTHOR` 등)는 명세의 이름을 그대로 쓴다. 메시지 어미는 `~입니다.`를 유지한다.
- 검증 실패처럼 필드별 메시지가 필요할 때만 `ErrorResponse.of(ErrorCode, message, request)`를 쓴다.

## 예외 클래스
- 비즈니스 예외는 `Backend14thException` 하나로 처리한다. 새 예외 클래스는 꼭 필요할 때만 제안 후 만든다.
- `GlobalExceptionHandler`에 핸들러를 추가할 때는 `ErrorResponse.of(ErrorCode, request)` 형식을 유지한다.

## 엔티티
- `BaseEntity`를 상속한다. `createdAt`, `updatedAt`을 직접 선언하지 않는다.
- PK: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
- `@Setter`를 쓰지 않는다. 상태 변경은 의미 있는 이름의 메서드로 만든다. (예: `updateTitle(...)`)
- 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 권장한다.
- 연관관계는 기본 `FetchType.LAZY`로 둔다.
