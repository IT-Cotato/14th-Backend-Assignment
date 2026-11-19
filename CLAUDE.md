# 프로젝트 안내

## 프로젝트 개요
- 코테이토 전용 스터디 매칭 플랫폼이다. 네이버 카페에서 하던 스터디 신청을 옮겨 온다.
- 핵심 기능: 회원가입(기수, 파트)·로그인·로그아웃·마이페이지, 스터디 모집글 CRUD와 검색, 댓글(= 스터디 신청) CRUD.
- 별도의 가입 신청 기능은 없다. 모집글에 댓글을 남기는 것이 곧 신청이다.
- 모노레포다. 백엔드는 `backend/`, 프론트엔드는 `frontend/`에 있다.

| 구분 | 스택 |
| --- | --- |
| 백엔드 | Java 21, Spring Boot 4, Spring Data JPA, Spring Security, JWT(jjwt), MySQL, springdoc |
| 프론트엔드 | React, TypeScript, Vite, Tailwind CSS, React Router, axios, TanStack Query |
| 테스트 | JUnit 5 + Mockito, H2(테스트 전용), Playwright E2E |

## 어디서 무엇을 읽을지
- 도메인, 데이터 모델, 패키지 구조: `docs/project-structure.md`
- API 명세(13개)와 에러 코드: `docs/api-spec.md`
- ERD: `docs/erd/`
- 실행 환경과 검증 절차: `docs/development.md`
- 브랜치와 제출 규칙: `.claude/rules/workflow.md`
- 백엔드 수정: `.claude/rules/api.md` / 프론트 수정: `.claude/rules/frontend.md` / E2E: `.claude/rules/e2e.md`
- E2E 실행: `.claude/skills/run-e2e/SKILL.md` / 제출 전 점검: `.claude/skills/check-assignment/SKILL.md`

## 주요 명령어
백엔드는 `backend/`에서 Java 21로 실행한다. Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 쓴다.
`.env`는 `backend/.env`에 둔다. 키 이름은 `backend/.env.example`을 본다.

```sh
cd backend && ./gradlew test          # 단위 테스트 (H2, MySQL 불필요)
cd backend && ./gradlew bootRun       # 8080 포트
cd frontend && npm run dev            # 3000 포트
cd frontend && npm run build && npm run lint
cd frontend && npx playwright test    # 백엔드와 DB를 먼저 켠다
python -m unittest discover -s .claude/hooks/tests
```

## 작업 방식
- 구현 전에 현재 코드와 `docs/api-spec.md`를 확인한다. 명세에 있는 필드 이름과 에러 코드는 바꾸지 않는다.
- 백엔드는 기존 공통 응답(`ResponseUtils`, `ApiResponse`, `ErrorResponse`)과 `ErrorCode`를 재사용한다.
- API를 바꾸면 백엔드, 프론트 타입(`frontend/src/api/types.ts`), 명세 문서, E2E를 함께 고친다.
- 테이블이 바뀌면 엔티티, `docs/erd/`, `docs/api-spec.md`를 함께 고친다.
- 검증은 실행한 명령과 결과를 함께 남긴다. 확인하지 못한 항목은 구분한다.

## 영구 금지
- `.env`와 비밀번호, 토큰을 읽거나 출력하거나 커밋하지 않는다. E2E 비밀번호도 코드에 적지 않는다.
- `develop`과 개인 누적 브랜치에 작업 내용을 직접 push하지 않는다. 개인 누적 브랜치 최초 생성은 예외다.
- 강제 push, 승인 없는 DB 초기화, 다른 사람 브랜치 수정은 하지 않는다.
- 파트장 검토 전에 PR을 직접 병합하지 않는다.
- Next.js를 쓰지 않는다. 프론트는 Vite 기반 React SPA다.
