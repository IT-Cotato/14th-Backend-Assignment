# 아키텍처

## 패키지 구조 (도메인형)

```
com.cotato.backend14th/
├── Backend14thApplication.java     # @EnableJpaAuditing
├── domain/
│   └── {도메인}/
│       ├── controller/             # 요청/응답만 담당
│       ├── service/                # 비즈니스 로직, 트랜잭션
│       ├── repository/             # JpaRepository
│       ├── entity/                 # BaseEntity 상속
│       └── dto/                    # XxxRequest, XxxResponse
└── global/
    ├── config/                     # SwaggerConfig, CorsConfig, SecurityConfig
    ├── common/
    │   ├── entity/BaseEntity       # createdAt, updatedAt 자동 관리
    │   └── response/               # ApiResponse, ErrorResponse, ResponseUtils
    └── exception/                  # ErrorCode, Backend14thException, GlobalExceptionHandler
```

- 특정 도메인에 속하지 않는 공통 코드는 `global`에 둔다.
- `HelloController`(루트), `domain/book`은 1~3주차 연습용이다.

## 계층 책임

| 계층 | 책임 | 하지 않는 것 |
| --- | --- | --- |
| Controller | 요청 받기, DTO 검증, `ResponseUtils`로 응답 | 비즈니스 로직, Repository 직접 호출 |
| Service | 비즈니스 로직, 트랜잭션, 예외 던지기 | HTTP 관련 처리 |
| Repository | DB 접근 | 로직 |

> `BookController`는 2~3주차 실습 편의상 Repository를 직접 호출한다. 새 도메인부터는 Service를 둔다.

## 성공 응답: `ApiResponse<T>`

```json
{
  "success": true,
  "status": 200,
  "data": { "id": 1, "title": "클린 코드", "author": "로버트 마틴" },
  "timestamp": "2026-08-20 14:30:00"
}
```

- 조회/수정: `ResponseUtils.ok(data)` → 200
- 생성: `ResponseUtils.created(data)` → 201
- `data`가 null이면 필드가 생략된다. (`@JsonInclude(NON_NULL)`)

## 실패 응답: `ErrorResponse`

```json
{
  "success": false,
  "status": 404,
  "code": "BOOK-001",
  "message": "책을 찾을 수 없습니다.",
  "method": "GET",
  "path": "/books/999",
  "timestamp": "2026-08-20 14:31:00"
}
```

### 예외 처리 흐름

```
Service/Controller에서 throw new Backend14thException(ErrorCode.XXX)
  → GlobalExceptionHandler(@RestControllerAdvice)가 잡음
  → ErrorResponse.of(errorCode, request)로 변환
  → ErrorCode의 HttpStatus로 응답
그 외 예상하지 못한 예외 → COMMON-002 (500)
```

### ErrorCode 규칙

- 모든 에러 코드는 `ErrorCode` enum 하나에 모은다.
- 코드 형식: `도메인 prefix-세 자리 번호` (예: `COMMON-001`, `BOOK-001`, `APPLICANT-001`)
- 상수 이름: `도메인_상황` (예: `APPLICANT_NOT_FOUND`)
- 새 상수는 마지막 `;` **앞에** 추가한다.

| 코드 | 상수 | HTTP |
| --- | --- | --- |
| COMMON-001 | COMMON_BAD_REQUEST | 400 |
| COMMON-002 | COMMON_INTERNAL_SERVER_ERROR | 500 |
| BOOK-001 | BOOK_NOT_FOUND | 404 |

## 엔티티 공통

- 모든 엔티티는 `BaseEntity`를 상속해서 `createdAt`, `updatedAt`을 자동으로 관리한다.
- `BaseEntity` 적용 전에 저장된 row는 두 값이 NULL일 수 있다. (정상)

## 설정 구조

| 파일 | 내용 |
| --- | --- |
| `application.yml` | 공통 설정. `.env` 로드(`spring.config.import`), 프로필 선택(기본 `local`) |
| `application-local.yml` | local 전용. DB 연결(`${DB_URL}` 등), `ddl-auto: update`, `show-sql: true` |
| `.env` | 실제 값. Git에 올리지 않음 |

`.env`에 필요한 키 (값은 각자 로컬 환경에 맞게 채운다)

```
DB_URL=jdbc:mysql://localhost:3306/backend14th
DB_USERNAME=
DB_PASSWORD=
```

## 보안/CORS

- `SecurityConfig`: CSRF·formLogin·httpBasic 비활성화, STATELESS, `anyRequest().permitAll()`
    - 인증 기능을 구현하기 전까지 permitAll을 유지한다.
- `CorsConfig`: 허용 출처는 `http://localhost:3000`, `http://localhost:8080`
    - 출처를 추가할 때는 `ALLOWED_ORIGINS` 목록에 명시한다. `*`는 쓰지 않는다. (`allowCredentials(true)`와 함께 쓸 수 없음)
