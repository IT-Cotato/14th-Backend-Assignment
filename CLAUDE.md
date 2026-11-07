# CLAUDE.md

## 프로젝트 개요

코테이토 14기 백엔드 과제 저장소. Spring Boot 4.1.1 / Java 21 / Gradle.
베이스 패키지는 `com.cotato.backend14th`.
MySQL + JPA, Swagger(springdoc), Spring Security를 사용한다.

현재 `Book`은 Swagger와 예외 처리 구조를 검증하기 위한 연습용 도메인이며,
실제 서비스 도메인은 아직 정해지지 않았다.
공통 응답(`ApiResponse`), 전역 예외 처리(`GlobalExceptionHandler`),
`BaseEntity`(생성·수정 시각 자동 기록) 구조가 갖춰져 있다.

## 프로젝트 구조

    src/main/java/com/cotato/backend14th/
    ├── domain/<도메인>/
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   └── repository/
    └── global/
        ├── common/entity/      BaseEntity
        ├── common/response/    ApiResponse, ErrorResponse, ResponseUtils
        ├── config/             SwaggerConfig, SecurityConfig
        └── exception/          ErrorCode, Backend14thException, GlobalExceptionHandler

- 새 도메인은 `domain/<도메인>/` 아래에 위 네 패키지 구조를 그대로 만든다.
- 여러 도메인이 공유하는 코드만 `global/`에 둔다.

## 주요 명령어

- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 실행: `./gradlew bootRun`
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 작업 방식

- 큰 작업은 계획을 먼저 제시하고, 승인을 받은 뒤에 실행한다.
- 요청이 모호하면 추측해서 진행하지 말고 질문한다.
- 요청한 범위 밖의 파일은 수정하지 않는다.
- 에러가 나면 스택트레이스의 마지막 `Caused by:`까지 확인하고 원인을 설명한다.
- 새 의존성을 추가하기 전에 먼저 이유를 설명하고 확인을 받는다.
- 파일이나 패키지를 새로 만들거나 삭제하기 전에 먼저 알리고 확인을 받는다.

## 영구 금지

- `.env` 파일을 읽거나 내용을 출력하지 않는다. 커밋 대상에 포함하지 않는다.
- 모든 `git` 명령을 실행하지 않는다. 상태 확인(`git status`, `git log`)도 하지 않는다.
  Git 관련 작업은 사람이 직접 한다. 필요하면 실행할 명령어를 알려주기만 한다.
- `build.gradle`의 Spring Boot 버전과 Java 버전을 변경하지 않는다.
  에러가 나더라도 버전 변경을 해결책으로 제안하지 않는다.