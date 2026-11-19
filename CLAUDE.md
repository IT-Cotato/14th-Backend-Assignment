# CLAUDE.md

## 프로젝트 개요

코테이토 14기 과제 저장소. 백엔드와 프론트엔드를 한 레포에서 관리하는 모노레포다.

- `backend/`: Spring Boot 4.1.1 / Java 21 / Gradle, 베이스 패키지 `com.cotato.backend14th`.
  MySQL + JPA, Swagger(springdoc), Spring Security, JWT를 사용한다.
- `frontend/`: React + TypeScript + Vite + Tailwind CSS (Next.js는 사용하지 않는다).
  Playwright로 E2E 테스트를 한다.

도메인은 **코테이토 스터디 매칭 플랫폼**이다.

- 인증: 회원가입(기수·파트 입력), 로그인, 로그아웃, 내 정보
- 스터디 모집글: 작성, 목록(검색·필터·페이징), 상세, 수정, 삭제
- 댓글 = 스터디 신청: 작성, 목록, 수정, 삭제

API 명세는 `docs/api-spec.md`, ERD는 `docs/erd/`를 따른다.
`Book`은 Swagger와 예외 처리 구조를 검증하기 위한 연습용 도메인으로 남아 있다.
공통 응답(`ApiResponse`), 전역 예외 처리(`GlobalExceptionHandler`),
`BaseEntity`(생성·수정 시각 자동 기록) 구조가 갖춰져 있다.

## 프로젝트 구조

    14th-Backend-Assignment/
    ├── CLAUDE.md, .claude/, .github/, .dev/
    ├── docs/        erd/, api-spec.md, features/
    ├── backend/     src/, build.gradle, .env(커밋 금지), .env.example
    └── frontend/    src/, e2e/, package.json, playwright.config.ts, vite.config.ts

    backend/src/main/java/com/cotato/backend14th/
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

### 백엔드 (반드시 `backend/`에서 실행한다. `.env`를 실행 위치에서 읽는다)

- 빌드: `./gradlew build` (Windows: `.\gradlew.bat build`)
- 테스트: `./gradlew test`
- 실행: `./gradlew bootRun`
- Swagger UI: http://localhost:8080/swagger-ui/index.html

### 프론트엔드 (`frontend/`에서 실행한다)

- 의존성 설치: `npm install`
- 개발 서버: `npm run dev` (http://localhost:3000, 포트 고정)
- 빌드: `npm run build` (타입 검사 포함)
- 린트: `npm run lint` (oxlint)
- API 주소는 `VITE_API_BASE_URL` 환경변수로 넣는다. (`frontend/.env.example` 참고)

## 작업 방식

- 큰 작업은 계획을 먼저 제시하고, 승인을 받은 뒤에 실행한다.
- 요청이 모호하면 추측해서 진행하지 말고 질문한다.
- 요청한 범위 밖의 파일은 수정하지 않는다.
- 에러가 나면 스택트레이스의 마지막 `Caused by:`까지 확인하고 원인을 설명한다.
- 새 의존성을 추가하기 전에 먼저 이유를 설명하고 확인을 받는다.
- 파일이나 패키지를 새로 만들거나 삭제하기 전에 먼저 알리고 확인을 받는다.

## 영구 금지

- `.env` 파일(`backend/.env`)을 읽거나 내용을 출력하지 않는다. 커밋 대상에 포함하지 않는다.
- 모든 `git` 명령을 실행하지 않는다. 상태 확인(`git status`, `git log`)도 하지 않는다.
  Git 관련 작업은 사람이 직접 한다. 필요하면 실행할 명령어를 알려주기만 한다.
- `backend/build.gradle`의 Spring Boot 버전과 Java 버전을 변경하지 않는다.
  에러가 나더라도 버전 변경을 해결책으로 제안하지 않는다.