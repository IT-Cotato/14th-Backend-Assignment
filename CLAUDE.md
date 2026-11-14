# CLAUDE.md

이 파일은 프로젝트 지도입니다. 핵심만 적고, 자세한 설명은 `docs/`에 둡니다.

## 1. 프로젝트 개요

- Cotato 14기 백엔드 스터디 과제입니다. 현재 Book 도메인 예제와 공통 기반이 구현되어 있습니다.
- 모노레포입니다. `backend/`에 Spring Boot API, `frontend/`에 Vite React SPA가 있습니다.
- API: `GET /hello`, `GET /books/{id}`, `POST /books`
- 기본 포트: 백엔드 8081 (`local` 프로필, 환경변수 `SERVER_PORT`로 변경 가능), 프론트 3000
- Swagger: `http://localhost:8081/swagger-ui/index.html`

## 2. 기술 스택

- Java 21, Spring Boot 4.1.1, Gradle 9.7.1 (Wrapper)
- Spring Web MVC, Data JPA, Validation, Security, Lombok
- MySQL, springdoc-openapi 2.8.5
- 프론트: Vite, React, TypeScript, Tailwind CSS, React Router, axios, Playwright

## 3. 주요 명령어 (Windows)

### 백엔드

`backend/`에서 실행합니다. `application.yml`이 작업 디렉터리 기준으로 `.env`를 읽기 때문입니다(`backend/.env`).

```powershell
cd backend
.\gradlew.bat build      # 빌드 + 테스트
.\gradlew.bat bootRun    # 실행
.\gradlew.bat test       # 테스트
.\gradlew.bat bootJar    # 테스트 없이 jar만 빌드
```

**주의:** 테스트와 빌드는 로컬 MySQL과 `backend/.env`가 필요하고, `ddl-auto: update`로 실제 로컬 DB 스키마를 바꿀 수 있습니다. 환경변수 이름은 `backend/.env.example`에 있습니다.

### 프론트엔드

`frontend/`에서 실행합니다.

```powershell
cd frontend
npm install              # 의존성 설치
npm run dev              # 개발 서버 (http://localhost:3000)
npm run build            # 타입 검사 + 빌드
npx playwright test      # E2E 테스트 (개발 서버 자동 실행)
```

## 4. 패키지 구조 요약

백엔드 코드는 `backend/src/main/java/com/cotato/backend14th/` 아래에 있습니다.

```text
com.cotato.backend14th
├─ domain/{도메인}/   controller, dto, entity, repository
└─ global/
   ├─ common/         BaseEntity, ApiResponse, ErrorResponse, ResponseUtils
   ├─ config/         Cors, Security, Swagger
   └─ exception/      ErrorCode, Backend14thException, GlobalExceptionHandler
```

현재 service 계층은 없고, 컨트롤러가 repository를 직접 호출합니다.

## 5. 코드와 Git 규칙

- Git: minho/basic 기준 작업, develop 대상 작업 금지 → [git-workflow](.claude/rules/git-workflow.md)
- API: ResponseUtils와 ErrorCode + Backend14thException 사용 → [api](.claude/rules/api.md)
- 엔티티: BaseEntity 상속, 변경 시 테스트 작성 → [entity](.claude/rules/entity.md)

## 6. 영구 금지 사항

- 보안: `.env` 접근과 비밀번호 직접 기입 금지 → [secrets](.claude/rules/secrets.md)
- Git: `develop` 대상 PR·커밋·푸시, 강제 푸시, 요청 없는 커밋·푸시 금지 → [git-workflow](.claude/rules/git-workflow.md)
- DB: 승인 없는 `DROP`, `TRUNCATE`, `DELETE`, 데이터 변경 요청 금지
- DB: `ddl-auto`를 `create`나 `create-drop`으로 변경 금지
- 엔티티: 테스트 없는 구조 변경 금지 → [entity](.claude/rules/entity.md)
- 범위: 승인 없는 의존성 추가와 버전 변경 금지

## 7. 디렉터리 구조

```text
14th-Backend-Assignment/
├── CLAUDE.md          프로젝트 지도
├── docs/              사람이 관리하는 문서
│   ├── erd/           ERD
│   ├── api-spec.md    API 명세
│   └── features/      PR(제출)별 정리 문서
├── .claude/
│   ├── rules/         상황별 규칙
│   ├── skills/        반복 작업 레시피
│   ├── hooks/         안전장치 스크립트
│   └── settings.json  훅 연결, 권한 설정
├── .dev/              AI 작업 기록 (learnings, scratchpad)
├── .github/           이슈/PR 템플릿
├── backend/           Spring Boot 프로젝트
│   ├── src/           애플리케이션 코드와 테스트
│   ├── build.gradle   빌드 설정
│   ├── .env.example   환경변수 이름 (값 없음)
│   └── .env           DB 접속 정보 (Git에 올리지 않음)
└── frontend/          Vite React TypeScript SPA
    ├── src/           화면 코드
    ├── e2e/           Playwright 테스트
    ├── playwright.config.ts
    └── vite.config.ts
```

## 8. 상세 문서

- [아키텍처](docs/architecture.md): 패키지 구조와 계층 설명
- [컨벤션](docs/conventions.md): 응답 포맷, 에러 코드, 코드 규칙
- [데이터베이스](docs/database.md): DB 설정 방식과 스키마 변경 절차
- [Git 작업 절차](docs/git-workflow.md): 브랜치, 커밋, PR 절차
- [API 명세](docs/api-spec.md): 엔드포인트 명세
- [4주차 하네스 작업](docs/features/week4-harness.md): 이번 제출 정리
- 규칙: [git-workflow](.claude/rules/git-workflow.md), [secrets](.claude/rules/secrets.md), [api](.claude/rules/api.md), [entity](.claude/rules/entity.md)
- 스킬: [add-domain-api](.claude/skills/add-domain-api/SKILL.md), [pre-pr-check](.claude/skills/pre-pr-check/SKILL.md), [write-feature-doc](.claude/skills/write-feature-doc/SKILL.md)
