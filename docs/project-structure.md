# 프로젝트 구조

코테이토 14기 5·6주차 과제인 **스터디 매칭 플랫폼** 모노레포의 실제 폴더 구조와 각 구성 요소의 책임을 정리한 문서예요.
`seungju/monorepo` 브랜치의 코드를 기준으로 해요. 코드가 바뀌면 이 문서도 함께 갱신해요.

---

## 1. 기술 스택

| 구분 | 내용 | 근거 |
| --- | --- | --- |
| 백엔드 | Java 17, Spring Boot 3.5.16, Gradle Wrapper | `backend/build.gradle` |
| 영속성 | Spring Data JPA, MySQL (`mysql-connector-j`) | `backend/build.gradle` |
| 인증 | Spring Security, OAuth2 Resource Server(JWT, HS256), BCrypt | `JwtConfig`, `PasswordConfig`, `SecurityConfig` |
| 검증 | Jakarta Bean Validation | 요청 DTO |
| API 문서 | springdoc-openapi 2.8.5 (Swagger UI) | `SwaggerConfig` |
| 프론트엔드 | React 19, TypeScript, Vite 8, Tailwind CSS 4, React Router, Axios | `frontend/package.json` |
| 정적 검사 | oxlint (프론트엔드), `tsc -b` 타입 검사 | `frontend/.oxlintrc.json` |
| 테스트 | JUnit 5 + Spring Boot Test (백엔드), Playwright (E2E) | `backend/src/test`, `frontend/e2e` |

---

## 2. 최상위 폴더 구조

```
14th-Backend-Assignment/
├── .claude/                  # Claude Code 하네스 (rules, hooks, skills, settings.json)
├── .github/                  # Issue / PR 템플릿
├── backend/                  # Spring Boot API 서버
│   ├── build.gradle, settings.gradle, gradlew, gradlew.bat, gradle/wrapper/
│   ├── .env.example          # 필요한 환경 변수 이름 (실제 값은 backend/.env, 커밋 금지)
│   └── src/
│       ├── main/java/com/cotato/backend14th/
│       ├── main/resources/   # application.yml, application-local.yml
│       └── test/java/com/cotato/backend14th/
├── frontend/                 # React + Vite 앱
│   ├── src/                  # 화면, API 호출, 인증 상태
│   ├── e2e/                  # Playwright E2E 테스트
│   ├── playwright.config.ts
│   └── package.json
├── docs/
│   ├── api-spec.md           # API 명세 (13개 API)
│   ├── features.md           # 기능 문서
│   ├── project-structure.md  # 이 문서
│   ├── coding-conventions.md
│   ├── erd/erdCloud.png      # ERD 이미지
│   └── screenshots/          # E2E로 찍은 주요 화면
└── CLAUDE.md                 # 프로젝트 전역 지침
```

---

## 3. 백엔드 패키지 구조

기본 패키지는 `com.cotato.backend14th`이고, 5·6주차 기능은 **Controller → Service → Repository** 구조를 따라요.

```
com.cotato.backend14th
├── Backend14thApplication.java   # 진입점 (@EnableJpaAuditing)
├── HelloController.java          # 1주차 레거시 (GET /hello, 평문 응답)
├── domain/
│   ├── auth/       controller, dto, entity(RevokedToken), repository, service(AuthService, TokenRevocationService)
│   ├── user/       controller(UserController), dto(UserResponse, AuthorResponse), entity(User, Part), repository, service
│   ├── studypost/  controller, dto, entity(StudyPost, StudyStatus), repository, service
│   ├── comment/    controller, dto, entity(StudyPostComment), repository, service
│   └── book/       1~3주차 레거시 (Controller가 Repository를 직접 호출, 그대로 유지)
└── global/
    ├── common/     entity(BaseEntity), response(ApiResponse, ErrorResponse, PageResponse, ResponseUtils)
    ├── config/     CorsConfig, JwtConfig, PasswordConfig, SecurityConfig, SwaggerConfig
    ├── exception/  ErrorCode, Backend14thException, GlobalExceptionHandler
    └── security/   JwtAuthenticationEntryPoint, JwtUtils, RevokedTokenValidator
```

### 도메인별 책임

| 도메인 | 책임 | API |
| --- | --- | --- |
| `auth` | 회원가입, 로그인(JWT 발급, 1시간 유효), 로그아웃(토큰 SHA-256 해시를 `revoked_token`에 저장) | `POST /api/auth/signup`, `/login`, `/logout` |
| `user` | 내 정보 조회, 작성자 정보 DTO | `GET /api/users/me` |
| `studypost` | 모집글 CRUD, 키워드·상태·파트 검색, 페이지네이션, 작성자 권한 검증 | `/api/study-posts`, `/api/study-posts/{postId}` |
| `comment` | 댓글(스터디 신청) CRUD, 마감된 글 작성 차단(409), 작성자 권한 검증 | `/api/study-posts/{postId}/comments`, `/api/comments/{commentId}` |
| `book` | 1~3주차 실습 API (레거시) | `GET /books/{id}`, `POST /books` |

### 공통 구성 요소

| 파일 | 책임 |
| --- | --- |
| `ApiResponse`, `ResponseUtils` | 성공 응답 `{ success, status, data, timestamp }`. 컨트롤러는 `ResponseUtils.ok/created`로 반환해요. 204는 본문 없이 반환해요. |
| `PageResponse` | 목록 응답 `{ content, page, size, totalElements, totalPages }` |
| `ErrorResponse`, `ErrorCode`, `GlobalExceptionHandler` | 실패 응답 `{ success, status, code, message, method, path, timestamp }`. 검증·타입 오류는 400 `COMMON-003`, 없는 URL은 404 `COMMON-005`, 지원하지 않는 메서드는 405 `COMMON-006`이에요. |
| `SecurityConfig` | Stateless, CSRF·폼 로그인 비활성화. 공개: 회원가입·로그인, Swagger, 모집글 목록·상세, 댓글 목록, 레거시 API. 나머지는 JWT 필요. |
| `JwtConfig`, `RevokedTokenValidator` | HS256 JWT 인코더·디코더. 로그아웃한 토큰은 디코더 검증 단계에서 거부해요. |
| `JwtAuthenticationEntryPoint` | 인증 실패 시 401 `COMMON-004` `ErrorResponse` |
| `CorsConfig` | `http://localhost:3000`, `http://localhost:8080` 허용 (MVC 설정과 Security 설정 두 곳에 같은 목록 사용) |

### 설정

| 파일 | 내용 |
| --- | --- |
| `application.yml` | `backend/.env`를 읽고(`optional:file:.env[.properties]`), 프로필은 `SPRING_PROFILES_ACTIVE`(기본 `local`) |
| `application-local.yml` | MySQL 연결(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), `ddl-auto: update`, `show-sql: true` |

---

## 4. 프론트엔드 구조

```
frontend/src/
├── main.tsx, App.tsx      # 라우터와 헤더
├── api/                   # Axios 클라이언트(client.ts, JWT 자동 첨부·401 처리), 도메인별 API 함수, 오류 변환
├── auth/                  # AuthContext, AuthProvider(로그인 상태·로그아웃), RequireAuth(보호 라우트), useLogout
├── components/            # 공통 UI (폼 필드, 페이지네이션, 상태 배지, 댓글 영역 등)
├── pages/                 # 화면 단위 컴포넌트
├── types/                 # API 요청·응답 타입 (docs/api-spec.md와 동일)
└── utils/                 # 날짜 포맷, 경로 파라미터 파싱
```

| 경로 | 화면 | 접근 |
| --- | --- | --- |
| `/signup` | 회원가입 | 공개 |
| `/login` | 로그인 | 공개 |
| `/studies` | 모집글 목록 (검색·필터·페이지네이션) | 공개 |
| `/studies/:id` | 모집글 상세 + 댓글(스터디 신청) | 공개 (작성은 로그인) |
| `/studies/new` | 모집글 작성 | 로그인 |
| `/studies/:id/edit` | 모집글 수정 (모집 상태 변경 포함) | 로그인 + 작성자 |
| `/mypage` | 내 정보, 로그아웃 | 로그인 |

개발 서버는 3000번 포트를 쓰고, `/api` 요청은 Vite 프록시로 `http://localhost:8080`에 전달해요.

---

## 5. 테스트

| 위치 | 종류 | 실행 |
| --- | --- | --- |
| `backend/src/test` | 컨트롤러(`@WebMvcTest`), 서비스 단위, `@SpringBootTest` 통합 테스트 | `cd backend && ./gradlew test` (로컬 MySQL과 `backend/.env` 필요) |
| `frontend/e2e` | Playwright Chromium E2E (회원가입·로그인, 모집글, 댓글, 마이페이지·로그아웃, 로그아웃 실패, 모바일 레이아웃, 스크린샷) | `cd frontend && npm run test:e2e` |

E2E는 실제 백엔드와 프론트엔드를 사용해요. 이미 실행 중인 서버가 있으면 재사용하고, 없으면 `playwright.config.ts`의 `webServer`가 띄워요.
의도적인 장애(로그아웃 500·네트워크 오류)만 `page.route`로 만들어요.

---

## 6. 요청 처리 흐름

```
Browser (React, :3000)
  │  Axios, Authorization: Bearer <JWT>
  ▼
Vite 프록시 /api → :8080
  ▼
Spring Security (CORS, JWT 검증, 로그아웃 토큰 거부)
  ▼
Controller ── @Valid DTO 검증
  ▼
Service ── 비즈니스 규칙(작성자 확인, 모집 마감 확인), @Transactional
  ▼
Repository ── MySQL
  │
  ├─ 성공: ResponseUtils → ApiResponse<T>
  └─ 실패: Backend14thException(ErrorCode) → GlobalExceptionHandler → ErrorResponse
```
