# CLAUDE.md

## 프로젝트 개요

Cotato 14기 5-6주차 과제 — **스터디 매칭 플랫폼** 모노레포.

- **도메인:** 코테이토 회원이 스터디 모집글을 올리고, 다른 회원이 댓글로 참여를 신청한다.
  - 회원(`User`): 회원가입(이메일·비밀번호·이름·닉네임·기수 `generation`(1 이상)·파트 `part` = `PM`/`DE`/`BE`/`FE`), 로그인(JWT `accessToken` 하나), 로그아웃(토큰 블랙리스트), 내 정보 조회
  - 모집글(`StudyPost`): 작성·목록·상세·수정·삭제. `keyword`(제목+내용)·`status`(`RECRUITING`/`CLOSED`, 컬럼 `study_status`)·`part`(작성자 파트, User 조인) 필터, 페이징, 최신순
  - 댓글(`StudyPostComment`) = 스터디 신청: 작성·목록(오래된 순)·수정·삭제. `CLOSED` 모집글에 작성하면 409 `STUDY_POST_CLOSED`. 글 삭제 시 댓글도 삭제
- **권한:** 조회는 공개, 작성은 로그인, 수정·삭제는 작성자 본인(아니면 403)
- **API 규칙:** 모든 엔드포인트는 `/api`로 시작, 성공은 `ApiResponse`(204는 본문 없음), 실패는 `ErrorResponse` (`docs/architecture.md`)
- **응답 공통:** 페이지 응답은 `content`·`page`·`size`·`totalElements`·`totalPages`, 작성자는 `author { nickname, generation, part }`
- **연관관계는 모두 `LAZY`, Enum은 `@Enumerated(EnumType.STRING)`**
- **API 명세:** `docs/api-spec.md` / **ERD:** `docs/erd/`

## 모노레포 구조

```
.
├── backend/    # Spring Boot REST API (포트 8080)
├── frontend/   # React + TypeScript + Vite + Tailwind CSS + React Router (포트 3000)
├── docs/       # 컨벤션, 아키텍처, API 명세, ERD
└── .claude/    # 하네스 (rules, skills, hooks)
```

### backend

- **스택:** Java 21 · Spring Boot 4.1.1 · MySQL · Spring Data JPA · Spring Security(JWT) · BCrypt · SpringDoc OpenAPI · Lombok
- **패키지 루트:** `com.cotato.backend14th` (`backend/src/main/java/com/cotato/backend14th`)
- **프로필:** `local` (`backend/src/main/resources/application-local.yml`, 값은 `backend/.env` — 항목은 `backend/.env.example` 참고)

### frontend

- **스택:** React · TypeScript · Vite · Tailwind CSS v4 · React Router · axios (Next.js 사용 금지)
- 개발 서버는 `3000` 고정(`strictPort`), `/api` 요청은 `localhost:8080`으로 프록시
- E2E: Playwright (`frontend/e2e/`, `frontend/playwright.config.ts`) — 규칙은 `.claude/rules/e2e-conventions.md`
- 프론트 규칙은 `.claude/rules/frontend-conventions.md`

## 주요 명령어

```bash
# backend
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'   # 로컬 실행
cd backend && ./gradlew build                                              # 빌드
cd backend && ./gradlew test                                               # 테스트

# frontend
cd frontend && npm install           # 의존성 설치
cd frontend && npm run dev           # 개발 서버 (http://localhost:3000)
cd frontend && npm run build         # 타입 체크 + 빌드
cd frontend && npm run lint          # oxlint
cd frontend && npx playwright test   # E2E 테스트 (백엔드 + DB를 먼저 켜야 함)
```

## 항상 적용

@docs/conventions.md
@docs/architecture.md

## 문서 라우팅

| 이런 작업을 할 때 | 읽을 문서 |
|---|---|
| 코딩 스타일·네이밍·Lombok·커밋 메시지 | `docs/conventions.md` (항상 로드) |
| 패키지 구조·레이어·응답/예외 패턴 | `docs/architecture.md` (항상 로드) |
| API 엔드포인트·Request/Response | `docs/api-spec.md` |
| 테이블·연관관계 | `docs/erd/README.md` |

## 작업 방식

- 코드 작성 전에 **계획을 먼저 세우고 승인받은 후 실행**한다.
- 모호한 점이 있으면 추측하지 말고 **질문으로 명확하게** 한다.
- 작업 단위는 **API 1개씩**. 하나가 끝나면 다음 작업을 안내하고 승인을 기다린다.
- 진행 순서: 0 세팅 → 1 인증 → 2 모집글 → 3 댓글 → 4 E2E. 단계별로 커밋하고, 프로덕션 코드와 테스트 코드는 커밋을 분리한다.

## 영구 금지

- `.env` 파일을 git에 커밋하거나 내용을 출력하지 않는다. (`.env.example`은 커밋 가능, 값은 비워 둔다)
- 강제 push(`--force`, `-f`)를 실행하지 않는다.
- `main`, `develop` 브랜치에 직접 push하지 않는다.
- DB를 직접 조작하는 쿼리(DROP, TRUNCATE, DELETE without WHERE)를 실행하지 않는다.
- 사용자의 승인 없이 파일을 삭제하지 않는다.
