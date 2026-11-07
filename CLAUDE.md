# CLAUDE.md

## 프로젝트 개요
- Cotato 14기 백엔드 과제 레포 (`com.cotato.backend14th`)
- Spring Boot 4.1.1 / Java 21 / Gradle
- 주요 의존성: Spring Web MVC, Spring Data JPA, MySQL, Spring Security, springdoc-openapi(Swagger), Validation, Lombok
- 현재 도메인: `book` (Controller / DTO / Entity / Repository)
- 공통 응답 포맷(`ApiResponse`/`ErrorResponse`), 전역 예외 처리(`GlobalExceptionHandler`), CORS/Security 설정이 `global/` 패키지에 적용되어 있음
- DB 접속 정보 등 민감 값은 `.env`를 통해 주입(`application.yml`에서 `optional:file:.env[.properties]` import)

## 주요 명령어
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 로컬 실행: `./gradlew bootRun`
- Swagger 접속: `http://localhost:8080/swagger-ui/index.html`

## 영구 금지 규칙
- `.env`, `application-local.yml` 등 민감 정보가 담긴 파일은 절대 커밋/푸시하지 않는다.
- `git push --force`, `git reset --hard` 등 파괴적 git 명령은 사용자 승인 없이 실행하지 않는다.
- 공통 응답 포맷(`ApiResponse`/`ErrorResponse`)과 전역 예외 처리 구조를 임의로 변경하지 않는다.
- DB 스키마나 마이그레이션을 임의로 변경하지 않는다.
- 사용자가 명시적으로 요청하지 않은 리팩토링/구조 변경은 하지 않는다.
