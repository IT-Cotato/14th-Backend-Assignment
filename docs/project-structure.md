# 프로젝트 구조와 공통 코드

패키지 구조와, 1~3주차에 만든 공통 코드(응답, 예외, BaseEntity, 설정)를 정리한 문서예요.

## 패키지 구조 (도메인형)

```
src/main/java/com/cotato/backend14th/
├── Backend14thApplication.java     # @EnableJpaAuditing 포함
├── domain/
│   └── book/                       # 연습용 도메인 (2주차)
│       ├── controller/             # 요청/응답 처리
│       ├── dto/                    # XxxRequest, XxxResponse
│       ├── entity/                 # DB 테이블과 매핑되는 객체
│       └── repository/             # JpaRepository 상속
└── global/
    ├── common/
    │   ├── entity/BaseEntity.java
    │   └── response/               # ApiResponse, ErrorResponse, ResponseUtils
    ├── config/                     # SwaggerConfig, CorsConfig, SecurityConfig
    └── exception/                  # ErrorCode, Backend14thException, GlobalExceptionHandler
```

- 새 도메인은 `domain/` 아래에 같은 4개 폴더(controller, dto, entity, repository)로 만들어요.
- 특정 도메인에 속하지 않는 공통 코드는 `global/`에 둬요.
- 지금은 Controller가 Repository를 바로 호출하지만, 5주차부터는 Service 계층을 추가할 예정이에요.

## 응답 형식

### 성공 응답: `ApiResponse<T>`
```json
{ "success": true, "status": 200, "data": { ... }, "timestamp": "2026-11-05 21:00:00" }
```
- 컨트롤러에서는 `ResponseUtils`로 감싸서 돌려줘요.
  - 조회, 수정: `return ResponseUtils.ok(data);` → 200
  - 생성: `return ResponseUtils.created(data);` → 201
- 반환 타입은 `ResponseEntity<ApiResponse<XxxResponse>>`예요.

### 실패 응답: `ErrorResponse`
```json
{ "success": false, "status": 404, "code": "BOOK-001", "message": "책을 찾을 수 없습니다.",
  "method": "GET", "path": "/books/999", "timestamp": "2026-11-05 21:00:00" }
```
- 직접 만들지 않아요. 예외를 던지면 `GlobalExceptionHandler`가 자동으로 만들어줘요.

## 예외 처리

1. `ErrorCode` enum에 상수를 추가해요.
   ```java
   BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK-001", "책을 찾을 수 없습니다."),
   ```
   - 코드 형식: `도메인-일련번호` (예: `BOOK-002`, `APPLICANT-001`)
   - 새 상수는 마지막 `;` **앞**에 추가해요. `;` 뒤에 넣으면 컴파일 에러가 나요.
2. 코드에서 던져요.
   ```java
   throw new Backend14thException(ErrorCode.BOOK_NOT_FOUND);
   ```
3. `GlobalExceptionHandler`가 잡아서 `ErrorResponse`로 바꿔줘요.
   - `Backend14thException` → ErrorCode에 적힌 상태 코드
   - 그 외 모든 예외 → 500 (`COMMON-002`)

## BaseEntity (JPA Auditing)

- `createdAt`, `updatedAt`을 자동으로 채워주는 공통 상위 클래스예요.
- 엔티티는 `extends BaseEntity`로 상속해요.
- `Backend14thApplication`에 `@EnableJpaAuditing`이 있어야 동작해요.
- BaseEntity 적용 전에 저장된 데이터는 시각이 `NULL`일 수 있어요. 정상이에요.

## 설정 (`global/config`)

| 파일 | 하는 일 |
|---|---|
| `SwaggerConfig` | Swagger 문서 제목, 설명, 버전 |
| `CorsConfig` | 프론트엔드 주소(예: `http://localhost:3000`)에서 오는 요청 허용 |
| `SecurityConfig` | Spring Security 설정. 지금은 `anyRequest().permitAll()`로 전부 열어둠 |

## 환경 설정

- `application.yml`: 공통 설정. `.env`를 읽어오고, 프로필 기본값은 `local`
- `application-local.yml`: 로컬 DB 설정. 값은 `${DB_URL}`처럼 `.env`에서 가져와요
- `.env`: DB 접속 정보. Git에 올리지 않아요 (`.gitignore`에 등록됨)
