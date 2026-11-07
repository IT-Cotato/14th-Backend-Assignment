# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

COTATO 14th backend study assignment: a Spring Boot 4.1 / Java 21 REST API (Gradle, JPA + MySQL, Spring Security, springdoc Swagger, Lombok). Base package `com.cotato.backend14th`.

## Commands

Use the Gradle wrapper (`./gradlew` in Git Bash, `.\gradlew.bat` in PowerShell).

- Build: `./gradlew build`
- Run: `./gradlew bootRun` (Swagger UI at `http://localhost:8080/swagger-ui/index.html`)
- All tests: `./gradlew test`
- Single test: `./gradlew test --tests "com.cotato.backend14th.Backend14thApplicationTests"` (append `.methodName` for one method)

No linter/formatter is configured.

## Configuration

- `application.yml` imports `.env` from the project root (`optional:file:.env[.properties]`) and activates the profile from `SPRING_PROFILES_ACTIVE`, defaulting to `local`.
- `application-local.yml` builds the MySQL datasource from `DB_URL` (required), `DB_USERNAME` (defaults to `root`) and `DB_PASSWORD` (defaults to empty), supplied by `.env`, which is gitignored. `ddl-auto: update`, `show-sql: true`.
- The only test is a `@SpringBootTest` context-load test, so `./gradlew test` needs a reachable MySQL and a valid `.env`.

## Architecture

Code is split into `domain/<name>/` (feature packages with `controller`, `dto`, `entity`, `repository`) and `global/` (cross-cutting code). At the moment there is no service layer: `BookController` calls `BookRepository` directly.

Conventions that span several files:

- **Success responses**: controllers return `ResponseEntity<ApiResponse<T>>` built via `ResponseUtils.ok(data)` / `ResponseUtils.created(data)`. `ApiResponse` wraps `success`, `status`, `data`, and a `timestamp` formatted in Asia/Seoul time.
- **Errors**: throw `Backend14thException(ErrorCode.X)`. `GlobalExceptionHandler` converts it into an `ErrorResponse` (status, code, message, method, path). Any other exception becomes `COMMON_INTERNAL_SERVER_ERROR`. For a new error, add an `ErrorCode` enum constant grouped by domain, with codes like `BOOK-001`, `COMMON-002`. There is no handler yet for Spring's own exceptions (`MethodArgumentNotValidException`, `HttpMessageNotReadableException`, 404s, and so on), so they currently come back as 500. `spring-boot-starter-validation` is on the classpath, but no DTO uses `@Valid` yet. Adding validation means adding a handler that maps to `COMMON_BAD_REQUEST`.
- **Entities** extend `BaseEntity` to get `createdAt`/`updatedAt` through JPA auditing (`@EnableJpaAuditing` is on `Backend14thApplication`).
- **Security** (`SecurityConfig`): stateless, CSRF/form login/basic auth disabled, `anyRequest().permitAll()` for now. CORS is defined twice in `CorsConfig`, once as a `WebMvcConfigurer` mapping and once as the `CorsConfigurationSource` bean used by Security. When changing allowed origins or methods, update both (they share `ALLOWED_ORIGINS`).
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
