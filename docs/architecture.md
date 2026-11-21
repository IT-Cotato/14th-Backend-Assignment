# 아키텍처

## 레포 구조 (모노레포)

```
14th-Backend-Assignment/
├── backend/          # Spring Boot 3.5 / Java 17. ./gradlew 는 여기서 실행한다
│   ├── src/
│   ├── build.gradle
│   └── .env.example  # 환경 변수 이름만 적는다 (값은 비운다)
├── frontend/         # React + TypeScript + Vite + Tailwind CSS
│   ├── src/
│   └── e2e/          # Playwright E2E
└── docs/             # architecture, conventions, api-spec, erd, features
```

## Backend 패키지 구조

```
com.cotato.backend14th
├── Backend14thApplication.java     # @EnableJpaAuditing
├── global/                         # 도메인과 무관한 공통 코드
│   ├── config/                     # SecurityConfig, CorsConfig, SwaggerConfig
│   ├── security/                   # JwtProvider, JwtAuthenticationFilter, JwtAuthenticationEntryPoint
│   ├── exception/                  # ErrorCode, Backend14thException, GlobalExceptionHandler
│   └── common/
│       ├── response/               # ApiResponse, ErrorResponse, PageResponse, ResponseUtils
│       └── entity/                 # BaseEntity (createdAt, updatedAt)
└── domain/                         # 기능(도메인)별 패키지
    └── <도메인>/
        ├── controller/             # HTTP 진입점, Swagger 문서화
        ├── service/                # 비즈니스 로직
        ├── repository/             # Spring Data JPA
        ├── dto/                    # 요청/응답 DTO
        └── entity/                 # JPA 엔티티
```

| 도메인 | 역할 |
| --- | --- |
| `auth` | 회원가입, 로그인, 로그아웃. 로그아웃한 토큰(`LogoutToken`) 저장 |
| `user` | `User` 엔티티(기수, 파트), 내 정보 조회, 작성자 정보 DTO(`AuthorResponse`) |
| `studypost` | 스터디 모집글 작성, 목록(검색, 필터, 페이징), 상세, 수정, 삭제 |
| `comment` | 댓글(스터디 신청) 작성, 목록, 수정, 삭제 |

- 새 도메인은 `domain/<도메인>/` 아래에 같은 하위 구조로 만든다.
- `global/`에는 두 개 이상의 도메인이 같이 쓰는 코드만 둔다.

## 요청 처리 흐름

### 성공
```
Client -> SecurityFilterChain (CORS, JwtAuthenticationFilter)
       -> Controller -> Service -> Repository
       -> ResponseUtils.ok(dto) / ResponseUtils.created(dto) / ResponseUtils.noContent()
       -> ApiResponse<T> { success, status, data, timestamp }   (204 는 본문 없음)
```

### 인증
```
Authorization: Bearer <accessToken>
  -> JwtAuthenticationFilter
       토큰이 유효하고 logout_token 에 없으면 userId 를 인증 정보로 넣는다
       토큰이 없거나 유효하지 않으면 익명으로 통과시킨다
  -> 인증이 필요한 경로인데 익명이면 JwtAuthenticationEntryPoint 가 401 ErrorResponse 를 내려준다
  -> Controller 는 @AuthenticationPrincipal Long userId 로 받는다 (익명이면 null)
```

- 공개: `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/study-posts/**`
- 그 외 `/api/**` 는 로그인 필요. 수정과 삭제는 서비스에서 작성자 본인인지 한 번 더 확인한다 (403).
- 공개 조회 API 에서도 토큰이 있으면 `isAuthor` 를 계산한다.
- 로그아웃: 사용한 토큰을 만료 시각과 함께 `logout_token` 테이블에 저장한다. 같은 토큰으로 다시 요청하면 401 이다.

### 실패
```
어디서든 throw new Backend14thException(ErrorCode.X)
  -> GlobalExceptionHandler.handleBackend14thException (log.warn)
  -> ErrorResponse { success=false, status, code, message, method, path, timestamp }

요청 값 검증 실패 (@Valid, 잘못된 JSON/enum, 파라미터 타입 불일치)
  -> 400 INVALID_INPUT (본문 검증 실패는 첫 번째 필드 메시지를 message 로 내려준다)

예상하지 못한 Exception
  -> GlobalExceptionHandler.handleException (log.error)
  -> ErrorCode.COMMON_INTERNAL_SERVER_ERROR (500)
```

## 응답 예시

성공
```json
{ "success": true, "status": 200, "data": { "postId": 10 }, "timestamp": "2026-11-19 14:00:00" }
```

실패
```json
{ "success": false, "status": 404, "code": "STUDY-001", "message": "존재하지 않는 스터디 모집글입니다.", "method": "GET", "path": "/api/study-posts/999", "timestamp": "2026-11-19 14:00:00" }
```

- 응답 래퍼의 `timestamp` 는 `yyyy-MM-dd HH:mm:ss`, 데이터 안의 `createdAt`/`updatedAt` 은 `yyyy-MM-dd'T'HH:mm:ss` 형식이다.

## Frontend 구조

```
frontend/src
├── api/         # axios 인스턴스(Bearer 헤더, 401 처리)와 도메인별 API 함수
├── auth/        # 로그인 상태(AuthContext), 로그인 필요 화면 보호(ProtectedRoute)
├── components/  # Header, 페이지네이션, 댓글 섹션 등
└── pages/       # 회원가입, 로그인, 스터디 목록/상세/작성/수정, 마이페이지
```

- 토큰은 `localStorage` 에 저장한다. 401 이 오면 토큰을 지우고 로그인 화면으로 보낸다.
- 서버가 내려준 에러 `message` 를 화면에 그대로 보여준다.

## 설정
- `backend/src/main/resources/application.yml`: `.env`를 `optional:file:.env[.properties]`로 import (실행 위치 기준이라 `backend/`에서 실행한다), 기본 profile `local`, `jwt.secret`, `jwt.access-expiration-ms`
- `application-local.yml`: MySQL 접속(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), `ddl-auto: update`, `show-sql: true`
- Security: CSRF/formLogin/httpBasic 비활성, STATELESS, BCrypt `PasswordEncoder`
- CORS: `http://localhost:3000`, `http://localhost:8080` 허용
