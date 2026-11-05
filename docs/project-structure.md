# 프로젝트 구조

기본 패키지: `src/main/java/com/cotato/backend14th`

| 경로 | 역할 |
| --- | --- |
| `Backend14thApplication.java` | 애플리케이션 진입점 |
| `HelloController.java` | 기본 응답 확인 |
| `domain/book/controller` | 도서 등록·단건 조회 |
| `domain/book/dto` | 도서 요청·응답 데이터 |
| `domain/book/entity` | 도서 JPA 엔티티 |
| `domain/book/repository` | DB 접근 |
| `global/common/entity` | 생성·수정 시각을 관리하는 BaseEntity |
| `global/common/response` | ApiResponse, ErrorResponse, ResponseUtils |
| `global/exception` | ErrorCode, 사용자 예외, 전역 예외 처리 |
| `global/config` | Swagger, CORS, Security 설정 |
| `src/main/resources` | 프로필과 DB 설정 |
| `src/test` | Spring 테스트 |

## 요청 처리

현재는 `BookController → BookRepository → MySQL` 순서로 처리한다. Service 계층은 아직 없다. 도메인 로직이 늘어날 때 분리 여부를 판단한다.

| 메서드 | 경로 | 처리 |
| --- | --- | --- |
| GET | `/hello` | 기본 응답 |
| POST | `/books` | title, author를 받아 저장하고 201 응답 |
| GET | `/books/{id}` | 도서 조회, 없으면 BOOK_NOT_FOUND |

성공 응답은 `ResponseUtils`로 감싼 `ApiResponse`를 사용한다. 업무 예외는 `Backend14thException`과 `ErrorCode`로 표현하고 `GlobalExceptionHandler`에서 HTTP 상태와 오류 응답으로 변환한다. 예상하지 못한 예외는 공통 500 응답을 반환한다.

현재 요청 DTO에 입력 검증이 적용되어 있다고 가정하지 않는다. 검증을 추가할 때 DTO 제약과 Controller의 `@Valid`, 실패 응답을 함께 확인한다.

## 문서와 설정

- `docs/`: 구조, 개발 기준 등 사람이 검토하는 문서
- `docs/features/`: 기능 제출 시 `이슈번호-작업명.md`로 배경·구현·API·설계·검증 기록
- `.claude/rules/`: 작업 중 지킬 규칙
- `.claude/skills/`: 필요할 때 사용하는 점검 절차
- `.claude/hooks/`: 도구 실행 전에 적용할 스크립트
- `.dev/`: 필요할 경우 임시 작업 기록을 두는 곳. 공식 요구사항으로 사용하지 않는다.
