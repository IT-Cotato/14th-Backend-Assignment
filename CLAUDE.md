# CLAUDE.md

## 프로젝트 개요

- 코테이토 14기 백엔드 과제 프로젝트
- 스택: Java 17 · Spring Boot 4.1.1 · Gradle · MySQL 8.0 · Spring Data JPA · springdoc-openapi 2.8.5 · Lombok
- 베이스 패키지: `com.cotato.backend14th` (`domain/` 기능 단위, `global/` 공통)
- 현재 상태: Book API(`GET /books/{id}`, `POST /books`), Swagger, 공통 응답·예외, JPA Auditing, CORS, Security(전체 permitAll)
- Service 계층 없음. Controller가 Repository를 직접 호출

## 어디서 무엇을 읽을지

- 새 도메인·패키지 추가 → `docs/architecture.md`
- 응답·에러·DTO·엔티티 작성 → `docs/conventions.md`
- 에러 추가·수정 → `global/exception/ErrorCode.java`, `GlobalExceptionHandler.java`
- 응답 형식 → `global/common/response/`
- 도메인·엔드포인트 작성 예시 → `domain/book/`
- DB·프로필 설정 → `src/main/resources/application.yml`, `application-local.yml`
- Swagger 설정 → `global/config/SwaggerConfig.java`

## 주요 명령어

- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 실행: `./gradlew bootRun`
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- 실행 전제: MySQL 기동, 루트 `.env`에 `DB_URL` `DB_USERNAME` `DB_PASSWORD`

## 작업 방식

- 요청 범위만 수정, 무관한 리팩터링 금지
- 코드 변경 후 `./gradlew build`로 확인
- 응답은 `ResponseUtils.ok`/`created`로 `ApiResponse`에 감싸 반환
- 에러는 `Backend14thException(ErrorCode.XXX)`로 throw, 새 에러는 `ErrorCode`에 추가
- 엔티티는 `BaseEntity` 상속
- Service 등 새 계층·추상화는 임의로 만들지 말고 제안만
- 커밋·push·PR은 요청 시에만
- 모르면 추측하지 말고 질문

## 영구 금지

- `.env` 커밋·출력 금지 (DB 접속 정보 포함)
- 엔티티를 Controller 응답으로 직접 반환 금지, DTO 사용
- `ddl-auto`를 `create`·`create-drop`으로 변경 금지 (데이터 삭제)
- 승인 없이 `git push --force`, `git reset --hard` 등 되돌리기 어려운 명령 금지
