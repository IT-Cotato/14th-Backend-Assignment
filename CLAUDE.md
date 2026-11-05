# CLAUDE.md

## 프로젝트 개요
- COTATO 14기 백엔드 과제 레포. Spring Boot 4.1.1, Java 21, Gradle, MySQL, Spring Data JPA
- 베이스 패키지: `com.cotato.backend14th` (도메인형 구조: `domain/{도메인}` + `global`)
- 주요 라이브러리: SpringDoc OpenAPI(Swagger), Spring Security(현재 전체 permitAll), Lombok, Validation
- 현재 상태: 공통 응답·예외 처리·CORS·Security·BaseEntity 세팅 완료. 도메인은 연습용 `Book`뿐
- 5주차부터 지원자관리 도메인(지원자, 서류, 운영진 등)을 개발할 예정
- 내 누적 브랜치: `soohyun/basic` (conventions.md의 `이름`은 `soohyun`이다)
- PR base는 항상 `soohyun/basic`이다. default 브랜치인 `develop`은 쓰지 않는다.

## 어디서 무엇을 읽을지
- 커밋, 브랜치, 이슈, PR 작성 → `docs/conventions.md`
- 패키지 구조, 계층 책임, 응답/에러 형식, 설정 구조 → `docs/architecture.md`
- 컨트롤러 작성 → `.claude/rules/controller.md` (해당 경로 작업 시 자동 적용)
- PR 기능 문서 작성 → `feature-doc` 스킬 (`docs/features/이슈번호-작업명.md`)

## 주요 명령어
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 컴파일만 빠르게 확인: `./gradlew compileJava`
- 실행: `./gradlew bootRun` (기본 프로필 `local`, `.env`에서 DB 정보 로드)
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 작업 방식
- 파일 3개 이상을 건드리는 작업은 먼저 계획을 보여주고 승인받은 뒤 진행한다.
- 요구사항이 모호하면 추측하지 말고 질문한다.
- 요청 범위 밖의 코드는 수정하지 않는다. 고칠 게 보이면 제안만 한다.
- 코드를 수정한 뒤에는 `./gradlew compileJava` 또는 `./gradlew build`로 확인하고 결과를 알린다.
- 새 도메인은 `domain/{도메인}/controller|service|repository|entity|dto` 구조로 만든다.
- 에러는 `global/exception/ErrorCode` enum 하나에 `도메인-번호` 형식으로 추가한다. (예: `APPLICANT-001`)
- `HelloController`(1주차), `Book`(2~3주차)은 연습용이다. `BookController`가 Repository를 직접 호출하는 건 예외이고, 새 코드에서 따라 하지 않는다.

## 영구 금지
- `.env`를 읽거나 수정하거나 커밋하지 않는다. yml에는 실제 값 대신 `${KEY}` 참조만 쓴다.
- `develop`(default), `이름/basic` 브랜치에 직접 push하지 않는다. force push도 하지 않는다. PR base는 `이름/basic`이다.
- `SecurityConfig`의 `anyRequest().permitAll()`을 임의로 바꾸지 않는다. (로그인 기능이 없어 모든 API가 401이 된다)
- CORS에서 `allowedOrigins("*")`와 `allowCredentials(true)`를 함께 쓰지 않는다.
- `RuntimeException`을 직접 던지지 않는다. `Backend14thException(ErrorCode.XXX)`를 쓴다.
- Entity를 API 응답으로 직접 반환하지 않는다. DTO로 변환한다.
- `build.gradle`의 의존성이나 버전을 묻지 않고 바꾸지 않는다.
