# 프로젝트 구조

## 도메인

코테이토 스터디 매칭 플랫폼이다. 회원이 스터디 모집글을 올리고, 다른 회원이 모집글에 댓글을 남기는 것으로 스터디를 신청한다.

| 용어 | 뜻 |
| --- | --- |
| 기수(`generation`) | 코테이토 기수. 1 이상의 정수, 가입 때 본인이 입력 |
| 파트(`part`) | `PM` 기획, `DE` 디자인, `BE` 백엔드, `FE` 프론트엔드 |
| 모집 상태(`studyStatus`) | `RECRUITING` 모집 중, `CLOSED` 모집 마감. 작성자가 자유롭게 바꾼다 |
| 댓글 | 스터디 신청. 마감된 글에는 쓸 수 없다 |

권한은 공개(조회), 로그인(작성), 작성자(수정·삭제) 세 단계다.

## 저장소

```
14th-Backend-Assignment/
├── CLAUDE.md, .claude/   하네스 (rules, skills, hooks)
├── .github/              이슈, PR 템플릿
├── docs/                 api-spec.md, erd/, features/, 개발 문서
├── backend/              Spring Boot (Gradle)
└── frontend/             React + Vite (e2e/ 에 Playwright)
```

## 백엔드

기본 패키지: `backend/src/main/java/com/cotato/backend14th`

| 경로 | 역할 |
| --- | --- |
| `domain/auth` | 회원가입, 로그인, 로그아웃. 로그아웃 토큰(`BlacklistedToken`) |
| `domain/user` | `User` 엔티티, `Part`, 내 정보 조회, 작성자 응답(`AuthorResponse`) |
| `domain/studypost` | 모집글 CRUD, 검색(키워드·상태·파트), `StudyStatus` |
| `domain/comment` | 댓글(신청) CRUD |
| `domain/book` | 1~3주차 실습 코드 |
| `global/security` | JWT 발급·검증, 인증 필터, 401 응답 |
| `global/config` | Security(권한), CORS(3000 허용), Swagger(Bearer) |
| `global/common` | `BaseEntity`, `ApiResponse`, `ErrorResponse`, `PageResponse`, `ResponseUtils` |
| `global/exception` | `ErrorCode`, `Backend14thException`, 전역 예외 처리 |

요청 흐름: `JwtAuthenticationFilter → Controller → Service → Repository → MySQL`

- 필터는 유효하고 로그아웃되지 않은 토큰이면 `userId`를 인증 정보로 등록한다. 토큰이 없거나 무효면 익명으로 넘기고, 로그인이 필요한 API에서 `JsonAuthenticationEntryPoint`가 401(`COMMON-004`)을 내려준다.
- 공개/로그인 구분은 `SecurityConfig`, 작성자 확인은 서비스(`isWrittenBy`)에서 한다.
- 목록 조회는 작성자를 fetch join하고, 댓글 수는 페이지 단위로 한 번에 센다.

## 프론트엔드

`frontend/src`

| 경로 | 역할 |
| --- | --- |
| `api/` | axios 클라이언트(토큰 헤더, 401 처리), API 함수, 응답 타입 |
| `auth/` | 토큰 저장, 로그인 상태(`AuthProvider`), 로그인 필요 라우트(`RequireAuth`) |
| `components/` | 헤더, 레이아웃, 공통 UI(에러, 로딩, 배지, 페이지네이션) |
| `pages/` | 회원가입, 로그인, 스터디 목록·상세·작성/수정, 마이페이지 |
| `e2e/` (frontend 바로 아래) | Playwright 테스트 |

| 화면 | 경로 |
| --- | --- |
| 회원가입 | `/signup` |
| 로그인 | `/login` |
| 스터디 목록 | `/studies` |
| 스터디 상세 | `/studies/:id` |
| 스터디 작성/수정 | `/studies/new`, `/studies/:id/edit` |
| 마이페이지 | `/mypage` |

## 문서와 설정

- `docs/api-spec.md`: API 13개 명세와 에러 코드
- `docs/erd/`: ERD 이미지, ERDCloud 링크, DDL
- `docs/features/`: PR 정리 문서 `이슈번호-기능명.md`
- `.claude/rules/`: 작업 중 지킬 규칙 (workflow, api, frontend, e2e)
- `.claude/skills/`: 제출 점검, E2E 실행 절차
- `.claude/hooks/`: 도구 실행 전 검사 스크립트
