# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

COTATO 14th study matching platform, organized as a monorepo: `backend/` is a Spring Boot 3.5 / Java 17 REST API (Gradle 8.14 wrapper, JPA + MySQL, Spring Security + OAuth2 Resource Server JWT, springdoc Swagger 2.8, Lombok), `frontend/` is React + TypeScript + Vite, and `docs/` holds the API spec and ERD. Base package `com.cotato.backend14th`.

## Commands

All backend commands run inside `backend/`. Use the Gradle wrapper (`./gradlew` in Git Bash, `.\gradlew.bat` in PowerShell).

- Clean build (includes tests): `./gradlew clean build`
- Build: `./gradlew build`
- Run: `./gradlew bootRun` (Swagger UI at `http://localhost:8080/swagger-ui/index.html`)
- All tests: `./gradlew test`
- Single test: `./gradlew test --tests "com.cotato.backend14th.Backend14thApplicationTests"` (append `.methodName` for one method)

No linter/formatter is configured.

## Configuration

- `application.yml` imports `.env` from the working directory, i.e. `backend/.env` (`optional:file:.env[.properties]`), and activates the profile from `SPRING_PROFILES_ACTIVE`, defaulting to `local`.
- `application-local.yml` builds the MySQL datasource from `DB_URL` (required), `DB_USERNAME` (defaults to `root`) and `DB_PASSWORD` (defaults to empty), supplied by `backend/.env`, which is gitignored. `JWT_SECRET` also comes from `.env`. `ddl-auto: update`, `show-sql: true`. `update` never changes the type of an existing column, so a column type change needs a manual `ALTER TABLE`.
- Tests live in `backend/src/test` (controller `@WebMvcTest`, service unit tests, and `@SpringBootTest` integration tests). The `@SpringBootTest` tests use the same local MySQL, so `./gradlew test` needs a reachable MySQL and a valid `backend/.env`.

## Architecture

Backend code is split into `domain/<name>/` (`auth`, `user`, `studypost`, `comment`, plus legacy `book`; each with `controller`, `dto`, `entity`, `repository`, `service`) and `global/` (`common`, `config`, `exception`, `security`). Weeks 5–6 code follows Controller → Service → Repository. The legacy `BookController` (weeks 1–3) still calls `BookRepository` directly and is left as is.

Conventions that span several files:

- **Success responses**: controllers return `ResponseEntity<ApiResponse<T>>` built via `ResponseUtils.ok(data)` / `ResponseUtils.created(data)`. `ApiResponse` wraps `success`, `status`, `data`, and a `timestamp` formatted in Asia/Seoul time.
- **Errors**: throw `Backend14thException(ErrorCode.X)`. `GlobalExceptionHandler` converts it into an `ErrorResponse` (status, code, message, method, path). Any other exception becomes `COMMON_INTERNAL_SERVER_ERROR`. For a new error, add an `ErrorCode` enum constant grouped by domain, with codes like `BOOK-001`, `COMMON-002`. `MethodArgumentNotValidException` (`@Valid` body), `HttpMessageNotReadableException` (bad JSON/enum), `MethodArgumentTypeMismatchException` (bad query/path type) and `HandlerMethodValidationException` (`@Min`/`@Max` on `@RequestParam`) all map to 400 `INVALID_INPUT` (`COMMON-003`). Unknown URLs (`NoResourceFoundException`) map to 404 `COMMON_NOT_FOUND` (`COMMON-005`), and unsupported HTTP methods (`HttpRequestMethodNotSupportedException`) map to 405 `METHOD_NOT_ALLOWED` (`COMMON-006`) with an `Allow` header. Security runs first, so these return 401 for protected paths when no token is sent.
- **Enums** are stored as strings with `@Enumerated(EnumType.STRING)` plus `@JdbcTypeCode(SqlTypes.VARCHAR)`, so the columns are `VARCHAR` as in the ERD rather than the MySQL `ENUM` type Hibernate would otherwise create (`User.part` VARCHAR(2), `StudyPost.studyStatus` VARCHAR(20)).
- **Entities** extend `BaseEntity` to get `createdAt`/`updatedAt` through JPA auditing (`@EnableJpaAuditing` is on `Backend14thApplication`).
- **Security** (`SecurityConfig`): stateless, CSRF/form login/basic auth disabled. JWT (HS256, `JwtConfig`) is verified through `oauth2ResourceServer().jwt()`; the user ID is the token `sub`, read in controllers with `@AuthenticationPrincipal Jwt`. Public: `POST /api/auth/signup|login`, Swagger, `GET /api/study-posts`, `GET /api/study-posts/*`, `GET /api/study-posts/*/comments`, and the legacy `GET /books/*`, `POST /books`, `GET /hello`; everything else requires a token. Authentication failures return 401 `ErrorResponse` (`COMMON-004`) via `JwtAuthenticationEntryPoint`. Logout (`POST /api/auth/logout`, 204) stores the token's SHA-256 hash in the `revoked_token` table (`TokenRevocationService`), and `RevokedTokenValidator`, attached to the `JwtDecoder` in `JwtConfig`, rejects revoked tokens. CORS is defined twice in `CorsConfig`, once as a `WebMvcConfigurer` mapping and once as the `CorsConfigurationSource` bean used by Security. When changing allowed origins or methods, update both (they share `ALLOWED_ORIGINS`).
- **Swagger**: annotate controllers with `@Tag` and endpoints with `@Operation`/`@Parameter`. The global info lives in `SwaggerConfig`.

Code comments and API descriptions are written in Korean, in a friendly `~해요` style. User-facing `ErrorCode` messages are the exception: they use the formal `~습니다` style (e.g. `"책을 찾을 수 없습니다."`).

`HelloController` (`GET /hello`, returns a plain String) is a leftover from week 1 and doesn't follow the `ApiResponse` convention.

## Git workflow

Weeks 1–3 used `feat/<issue#>-<topic>` branches with PRs into `develop` (e.g. `feat/84-response-security`). From week 4 on, work goes through a personal cumulative branch in the `IT-Cotato` org repo:

- Personal cumulative branch: `seungju/basic`.
- Week 4 work branch: `seungju/harness`. The week 4 PR must go from `seungju/harness` into `seungju/basic`.
- Weeks 5–6: create a single work branch `seungju/monorepo` from `seungju/basic` and open one PR back into `seungju/basic`.
- Commit messages use a type prefix that matches the change (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:` ...) followed by a Korean summary.
- PR descriptions follow `.github/PULL_REQUEST_TEMPLATE.md` and must include 작업 내용, 관련 이슈 (`closes #`), and 리뷰 포인트.

## Permanent prohibitions

- Never commit or expose secrets (`.env`, API keys, DB passwords, etc.) to Git or any external service.
- Never push directly to `main` or `develop`.
- Never force push or rewrite Git history (rebase, amend of pushed commits, reset) without the user's approval.
- Never mass-delete or reset existing files without the user's approval.
- Never make code changes outside the scope of the request.

## 프로젝트 개요

코테이토 14기 스터디 매칭 플랫폼 프로젝트이다.

사용자는 회원가입 및 로그인 후 스터디 모집글을 작성하고,
댓글을 통해 스터디에 신청할 수 있다.

## 모노레포 구조

- backend/: Java 17, Spring Boot 3.5.x, Spring Data JPA
- frontend/: React, TypeScript, Vite, Tailwind CSS
- docs/: ERD, API 명세, 기능 문서
- .claude/: Claude Code 하네스

백엔드와 프론트엔드는 하나의 Git 저장소에서 관리한다.

## 백엔드

- Spring Boot 코드는 backend/src/에 위치한다.
- Gradle 빌드 파일은 backend/build.gradle이다.
- 모든 API는 /api로 시작한다.
- 인증은 Spring Security와 JWT를 사용한다.
- 비밀번호는 BCrypt로 암호화한다.
- JPA 연관관계는 LAZY를 사용한다.
- Enum은 문자열로 저장한다.
- 기존 공통 응답 및 예외 처리 형식을 유지한다.

### 백엔드 테스트 (Windows)

```powershell
cd backend
.\gradlew.bat test
```

## 프론트엔드

- React와 TypeScript를 사용한다.
- Vite 개발 서버는 3000번 포트를 사용한다.
- Tailwind CSS로 스타일링한다.
- React Router로 페이지를 관리한다.
- Axios로 백엔드 API를 호출한다.

### 프론트엔드 실행

```powershell
cd frontend
npm install
npm run dev
```

### 프론트엔드 빌드

```powershell
cd frontend
npm run build
npm run lint
```

### E2E 테스트 (Playwright)

```powershell
cd frontend
npm run test:e2e
```

- 실제 백엔드(8080)와 프론트엔드(3000)를 사용하고, 실행 중인 서버는 재사용해요.
- `backend/.env`가 로컬 개발용 DB를 가리키는지 먼저 확인해요.
- 테스트 계정은 고정 풀(`e2e.pool.*@example.com`)을 재사용하고, 테스트가 만든 모집글만 정리해요. 계정 삭제 API는 없어요.

## 주요 기능

1. 회원가입, 로그인, 로그아웃, 내 정보 조회
2. 스터디 모집글 작성, 조회, 검색, 수정, 삭제
3. 댓글 작성, 조회, 수정, 삭제
4. Playwright를 이용한 E2E 테스트

## 개발 규칙

- 기존 4주차 하네스를 유지하고 확장한다.
- 기존 코드를 수정할 때 변경 영향을 확인한다.
- 백엔드와 프론트엔드 API 명세를 일치시킨다.
- 민감 정보와 .env 파일을 Git에 커밋하지 않는다.
- 기능 구현 후 관련 테스트를 실행한다.