# CLAUDE.md

이 파일은 프로젝트 지도입니다. 핵심만 적고, 자세한 설명은 `docs/`에 둡니다.

## 1. 프로젝트 개요

- Cotato 14기 백엔드 스터디 과제입니다. 현재 Book 도메인 예제와 공통 기반이 구현되어 있습니다.
- API: `GET /hello`, `GET /books/{id}`, `POST /books`
- 기본 포트: 8081 (`local` 프로필, 환경변수 `SERVER_PORT`로 변경 가능)
- Swagger: `http://localhost:8081/swagger-ui/index.html`

## 2. 기술 스택

- Java 21, Spring Boot 4.1.1, Gradle 9.7.1 (Wrapper)
- Spring Web MVC, Data JPA, Validation, Security, Lombok
- MySQL, springdoc-openapi 2.8.5

## 3. 주요 명령어 (Windows)

프로젝트 루트에서 실행합니다. `.env`를 상대 경로로 읽기 때문입니다.

```powershell
.\gradlew.bat build      # 빌드 + 테스트
.\gradlew.bat bootRun    # 실행
.\gradlew.bat test       # 테스트
.\gradlew.bat bootJar    # 테스트 없이 jar만 빌드
```

**주의:** 테스트와 빌드는 로컬 MySQL과 `.env`가 필요하고, `ddl-auto: update`로 실제 로컬 DB 스키마를 바꿀 수 있습니다.

## 4. 패키지 구조 요약

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

**코드**
- 성공 응답은 `ResponseUtils`로 반환합니다.
- 예외는 `ErrorCode`에 추가하고 `Backend14thException`으로 던집니다.
- 엔티티는 `BaseEntity`를 상속합니다.

**Git**
- 브랜치: `minho/basic`에서 `minho/{작업명}` 브랜치를 만들어 작업합니다. (예: `minho/harness`)
- 커밋: `feat:`, `fix:`, `chore:`, `docs:`
- PR: base는 반드시 `minho/basic`이고, `.github/PULL_REQUEST_TEMPLATE.md`를 사용합니다.

## 6. 영구 금지 사항

**보안**
- `.env`를 열거나 수정하거나 내용을 출력하지 않고, 커밋하지도 않습니다.
- 비밀번호나 토큰을 yml, 코드, 로그, 커밋에 직접 쓰지 않습니다. yml에서는 `${환경변수}` 참조만 사용합니다.

**Git**
- `develop`으로 PR을 보내거나 `develop`에 직접 커밋, 푸시하지 않습니다.
- 강제 푸시를 하지 않습니다.
- 사용자 요청 없이 커밋이나 푸시를 하지 않습니다.

**DB**
- 사용자 승인 없이 `DROP`, `TRUNCATE`, `DELETE`나 데이터를 바꾸는 요청을 실행하지 않습니다.
- `ddl-auto`를 `create`나 `create-drop`으로 바꾸지 않습니다.
- 테스트 없이 엔티티 구조를 변경하지 않습니다. 엔티티를 바꿀 때는 관련 테스트를 함께 작성합니다.

**범위**
- 승인 없이 의존성을 추가하거나 버전을 변경하지 않습니다.

## 7. 디렉터리 구조

```text
14th-Backend-Assignment/
├── CLAUDE.md          프로젝트 지도
├── docs/              사람이 관리하는 문서
│   └── features/      PR(제출)별 정리 문서
├── .claude/
│   ├── rules/         상황별 규칙
│   ├── skills/        반복 작업 레시피
│   ├── hooks/         안전장치 스크립트
│   └── settings.json  훅 연결, 권한 설정
├── .dev/              AI 작업 기록 (learnings, scratchpad)
├── .github/           이슈/PR 템플릿
├── src/               애플리케이션 코드와 테스트
├── build.gradle       빌드 설정
└── .env               DB 접속 정보 (Git에 올리지 않음)
```

## 8. 상세 문서

- [아키텍처](docs/architecture.md): 패키지 구조와 계층 설명
- [컨벤션](docs/conventions.md): 응답 포맷, 에러 코드, 코드 규칙
- [데이터베이스](docs/database.md): DB 설정 방식과 스키마 변경 절차
- [Git 작업 절차](docs/git-workflow.md): 브랜치, 커밋, PR 절차
- [4주차 하네스 작업](docs/features/week4-harness.md): 이번 제출 정리
- 규칙: [secrets](.claude/rules/secrets.md), [entity-changes](.claude/rules/entity-changes.md)
- 스킬: [add-domain-api](.claude/skills/add-domain-api/SKILL.md), [pre-pr-check](.claude/skills/pre-pr-check/SKILL.md)
