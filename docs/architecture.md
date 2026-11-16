# 아키텍처

## 패키지 구조 (도메인형)

코드 위치: `backend/src/main/java/com/cotato/backend14th/`

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
    ├── exception/                  # ErrorCode, Backend14thException, GlobalExceptionHandler
    └── security/                   # JwtProvider, JwtAuthenticationFilter, JwtAuthenticationEntryPoint, TokenBlacklist
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
- 코드 형식: `도메인 prefix-세 자리 번호` (예: `COMMON-001`, `BOOK-001`, `STUDY-001`)
- 상수 이름: `도메인_상황` (예: `STUDY_POST_NOT_FOUND`)
    - 예외: `INVALID_INPUT`, `UNAUTHORIZED`처럼 과제가 이름을 정한 상수는 도메인 접두어 규칙의 예외로, 과제 이름을 그대로 쓴다. (`docs/api-spec.md` 에러 코드 표 참고)
- 새 상수는 마지막 `;` **앞에** 추가한다.

| 코드 | 상수 | HTTP |
| --- | --- | --- |
| COMMON-001 | COMMON_BAD_REQUEST | 400 |
| COMMON-002 | COMMON_INTERNAL_SERVER_ERROR | 500 |
| COMMON-003 | INVALID_INPUT | 400 |
| COMMON-004 | UNAUTHORIZED | 401 |
| BOOK-001 | BOOK_NOT_FOUND | 404 |
| USER-001 | EMAIL_ALREADY_EXISTS | 409 |
| USER-002 | INVALID_CREDENTIALS | 401 |
| STUDY-001 | STUDY_POST_NOT_FOUND | 404 |
| STUDY-002 | NOT_POST_AUTHOR | 403 |
| STUDY-003 | STUDY_POST_CLOSED | 409 |
| COMMENT-001 | COMMENT_NOT_FOUND | 404 |
| COMMENT-002 | NOT_COMMENT_AUTHOR | 403 |

## 엔티티 공통

- 모든 엔티티는 `BaseEntity`를 상속해서 `createdAt`, `updatedAt`을 자동으로 관리한다.
- `BaseEntity` 적용 전에 저장된 row는 두 값이 NULL일 수 있다. (정상)

## 설정 구조

| 파일 | 내용 |
| --- | --- |
| `backend/src/main/resources/application.yml` | 공통 설정. `.env` 로드(`spring.config.import`), 프로필 선택(기본 `local`), JWT 설정(`jwt.secret: ${JWT_SECRET}`, `jwt.access-token-validity-seconds: 3600`) |
| `backend/src/main/resources/application-local.yml` | local 전용. DB 연결(`${DB_URL}` 등), `ddl-auto: update`, `show-sql: true` |
| `backend/.env` | 실제 값. Git에 올리지 않음 |

- `spring.config.import`는 `.env`를 실행 디렉터리 기준으로 읽는다. Gradle을 `backend/`에서 실행하므로 `.env`는 `backend/`에 둔다.

`.env`에 필요한 키 (값은 각자 로컬 환경에 맞게 채운다)

```
DB_URL=jdbc:mysql://localhost:3306/backend14th
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=                # HS256 서명용, 32바이트 이상 문자열
```

- 키 목록은 `backend/.env.example`에도 있다. (값 없이 키 이름만, Git에 올라감)
- `JWT_SECRET`이 없으면 서버 시작과 `contextLoads` 테스트가 실패한다. 32바이트보다 짧으면 시작할 때 `WeakKeyException`이 난다.

## 보안/CORS

- `SecurityConfig`: CSRF·formLogin·httpBasic 비활성화, STATELESS, JWT 인증
    - 접근 권한은 `docs/api-spec.md`의 권한 표를 따른다. 표에 없는 경로는 열지 않는다.
    - 공개: Swagger(`/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`), `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/study-posts`, `GET /api/study-posts/*`, `GET /api/study-posts/*/comments`
    - 그 외 경로는 전부 `authenticated()`이다. 연습용 `/hello`, `/books/**`도 토큰이 필요하다.
    - 작성자 확인(403)은 Security가 아니라 Service에서 `Backend14thException`으로 처리한다.
    - 비밀번호 암호화: `PasswordEncoder` 빈 = `BCryptPasswordEncoder`
- JWT (`global/security/`)
    - `JwtProvider`: 토큰 생성·검증. subject = userId, HS256, 유효시간은 `jwt.access-token-validity-seconds`(기본 3600초). 시크릿은 `jwt.secret: ${JWT_SECRET}`
    - `JwtAuthenticationFilter`: `Authorization: Bearer {token}`을 읽어 SecurityContext에 인증을 넣는다. principal = `Long userId`(`@AuthenticationPrincipal Long userId`로 꺼냄), credentials = 원본 토큰(로그아웃에 사용)
        - 토큰이 없거나 유효하지 않거나 로그아웃된 토큰이면 막지 않고 비로그인으로 넘긴다. 공개 API는 그대로 동작하고, 로그인 필요 경로만 401이 된다.
        - `@Component`가 아니다. 서블릿 필터로 중복 등록되지 않도록 `SecurityConfig`에서 직접 생성한다.
    - `TokenBlacklist`: 로그아웃한 토큰을 만료 시각까지 막는 인메모리 저장소. 서버를 재시작하면 비워진다.
    - `JwtAuthenticationEntryPoint`: Security 단계의 401을 `ErrorResponse`(`UNAUTHORIZED`, `COMMON-004`) 형식으로 내려준다. (`@RestControllerAdvice`를 거치지 않기 때문)
- Swagger: `bearerAuth` 스킴이 등록돼 있어 Authorize 버튼으로 토큰을 넣으면 모든 요청에 붙는다.
- `CorsConfig`: 허용 출처는 `http://localhost:3000`, `http://localhost:8080`
    - 출처를 추가할 때는 `ALLOWED_ORIGINS` 목록에 명시한다. `*`는 쓰지 않는다. (`allowCredentials(true)`와 함께 쓸 수 없음)
