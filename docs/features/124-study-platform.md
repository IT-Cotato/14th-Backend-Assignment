# 스터디 매칭 플랫폼 (회원, 모집글, 댓글)

- 이슈: #124
- PR:https://github.com/IT-Cotato/14th-Backend-Assignment/pull/125
- 작성일: 2026-11-16

## 배경
5~6주차 과제. 코테이토 스터디 매칭 플랫폼의 회원, 스터디 모집글, 댓글(=스터디 신청) 기능을 백엔드 API와 프론트 화면으로 구현한다.

## 구현 내용
기준 브랜치 `soohyun/basic` 대비 변경 범위 (커밋 목록은 PR 참고)

### 레포 구조
- 모노레포로 전환: 기존 백엔드를 `backend/`로 옮기고, `frontend/`(Vite, React, TypeScript, Tailwind)를 새로 만들었다.
- 하네스(`CLAUDE.md`, `.claude/settings.json`, `.claude/rules/`)의 경로를 모노레포 구조에 맞게 수정하고, E2E 작성 규칙(`.claude/rules/e2e.md`), 확인용 서버 종료 규칙, 답변 언어 규칙을 추가했다.
- 문서: `docs/api-spec.md`(API 13개), `docs/erd/` 작성

### 인증
백엔드

| 위치 | 주요 클래스 | 내용 |
| --- | --- | --- |
| domain/user | `AuthController`, `UserController`, `AuthService`, `UserService`, `User`, `Part` | 회원가입(BCrypt 암호화), 로그인(JWT 발급), 로그아웃, 내 정보 조회 |
| global/security | `JwtProvider`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`, `TokenBlacklist` | 토큰 발급·검증, 인증 실패 시 401 JSON 응답, 로그아웃 토큰 무효화 |
| global/config | `SecurityConfig` | 경로별 권한 설정 |

인증 흐름
1. 로그인 시 `JwtProvider`가 accessToken을 발급한다. (`tokenType: Bearer`)
2. 요청마다 `JwtAuthenticationFilter`가 `Authorization: Bearer {token}`을 검증하고, `TokenBlacklist`에 있는 토큰은 거부한다.
3. 인증이 필요한 경로에 토큰이 없거나 유효하지 않으면 `JwtAuthenticationEntryPoint`가 401 `COMMON-004`를 내려준다.
4. 작성자 확인은 Service에서 한다. (토큰 → 대상 조회 404 → 작성자 403 순서)

프론트엔드
- 페이지: `SignupPage`, `LoginPage`, `MyPage`
- 컴포넌트: `RequireAuth`(로그인 필요 페이지 보호)
- `auth/AuthProvider.tsx`로 로그인 상태 관리

### 모집글
백엔드

| 위치 | 주요 클래스 | 내용 |
| --- | --- | --- |
| domain/study | `StudyPostController`, `StudyPostService`, `StudyPost`, `StudyStatus` | 모집글 CRUD, 키워드·상태·파트 필터 + 페이징 목록 |

프론트엔드
- 페이지: `StudyListPage`, `StudyDetailPage`, `StudyFormPage`(작성/수정)
- 컴포넌트: `StatusBadge`

### 댓글
백엔드

| 위치 | 주요 클래스 | 내용 |
| --- | --- | --- |
| domain/comment | `CommentController`, `CommentService`, `StudyPostComment` | 댓글 작성(마감 글 불가), 목록, 수정, 삭제 |

프론트엔드
- 컴포넌트: `CommentSection`(댓글 작성·목록·수정·삭제)

### 공통
백엔드

| 위치 | 주요 클래스 | 내용 |
| --- | --- | --- |
| global | `GlobalExceptionHandler`, `PageResponse`, `ResponseUtils` | 검증 실패 400 처리 확장, 페이지 응답, 204 응답 |

프론트엔드
- 컴포넌트: `Header` 등
- `api/client.ts`에서 토큰을 헤더에 담아 호출

## API 명세

| 메서드 | URL | 권한 | 설명 | 요청 | 성공 응답 |
| --- | --- | --- | --- | --- | --- |
| POST | /api/auth/signup | 공개 | 회원가입 | `SignupRequest` (email, password, name, nickname, generation, part) | 201 `SignupResponse` |
| POST | /api/auth/login | 공개 | 로그인 | `LoginRequest` (email, password) | 200 `LoginResponse` (accessToken, tokenType) |
| POST | /api/auth/logout | 로그인 | 로그아웃 (토큰 무효화) | - | 204 |
| GET | /api/users/me | 로그인 | 내 정보 조회 | - | 200 `UserResponse` |
| POST | /api/study-posts | 로그인 | 모집글 작성 (처음 상태 RECRUITING) | `StudyPostCreateRequest` (title, content) | 201 `StudyPostIdResponse` |
| GET | /api/study-posts | 공개 | 모집글 목록 (최신순) | query: keyword, status, part, page=0, size=10 | 200 `PageResponse<StudyPostSummaryResponse>` |
| GET | /api/study-posts/{postId} | 공개 | 모집글 상세 (토큰 있으면 isAuthor 판단) | - | 200 `StudyPostDetailResponse` |
| PATCH | /api/study-posts/{postId} | 작성자 | 모집글 수정 (보낸 필드만) | `StudyPostUpdateRequest` (title, content, studyStatus) | 200 `StudyPostIdResponse` |
| DELETE | /api/study-posts/{postId} | 작성자 | 모집글 삭제 (댓글 함께 삭제) | - | 204 |
| POST | /api/study-posts/{postId}/comments | 로그인 | 댓글(신청) 작성 | `CommentRequest` (content) | 201 `CommentIdResponse` |
| GET | /api/study-posts/{postId}/comments | 공개 | 댓글 목록 (오래된 순) | query: page=0, size=20 | 200 `PageResponse<CommentResponse>` |
| PATCH | /api/comments/{commentId} | 작성자 | 댓글 수정 | `CommentRequest` (content) | 200 `CommentIdResponse` |
| DELETE | /api/comments/{commentId} | 작성자 | 댓글 삭제 | - | 204 |

주요 검증
- 회원가입: email 형식, password 8자 이상, nickname 2~10자, generation 1 이상, part 필수 (`PM`, `DE`, `BE`, `FE`)
- 모집글: title 50자 이하, content 2000자 이하. 수정 시 보낸 필드는 공백만 있으면 안 된다.
- 댓글: content 500자 이하, 공백 불가
- 페이징: page 0 이상, size 1~100

### 에러 케이스

| 상황 | HTTP | 코드 | 메시지 |
| --- | --- | --- | --- |
| 요청 본문/파라미터 검증 실패, 잘못된 enum 값, JSON 형식 오류 | 400 | COMMON-003 | 입력값이 올바르지 않습니다. |
| 토큰이 없거나 유효하지 않음, 로그아웃한 토큰, 토큰의 회원이 없음 | 401 | COMMON-004 | 인증이 필요합니다. |
| 이미 가입된 이메일로 회원가입 | 409 | USER-001 | 이미 가입된 이메일입니다. |
| 로그인 이메일 또는 비밀번호 불일치 | 401 | USER-002 | 이메일 또는 비밀번호가 올바르지 않습니다. |
| 없는 모집글 조회/수정/삭제, 없는 글에 댓글 작성·조회 | 404 | STUDY-001 | 스터디 모집글을 찾을 수 없습니다. |
| 작성자가 아닌 회원이 모집글 수정/삭제 | 403 | STUDY-002 | 모집글 작성자만 수정하거나 삭제할 수 있습니다. |
| 마감(CLOSED)된 모집글에 댓글 작성 | 409 | STUDY-003 | 모집이 마감된 글에는 댓글을 작성할 수 없습니다. |
| 없는 댓글 수정/삭제 | 404 | COMMENT-001 | 댓글을 찾을 수 없습니다. |
| 작성자가 아닌 회원이 댓글 수정/삭제 | 403 | COMMENT-002 | 댓글 작성자만 수정하거나 삭제할 수 있습니다. |

## 설계 결정
코드와 `docs/api-spec.md`에 적힌 선택만 기록했다.

- 명세에 없는 세부 규칙은 쉬운 쪽으로 구현한다. (모집 상태 RECRUITING ↔ CLOSED 자유 변경, 같은 사람의 댓글 여러 번 작성 허용)
- 토큰은 accessToken 하나만 쓴다. (refresh token 없음) 로그아웃은 `TokenBlacklist`(인메모리 `ConcurrentHashMap`)에 토큰을 만료 시각까지 저장해 막는다. 서버를 재시작하면 비워진다.
- 권한은 `SecurityConfig`에서 공개/로그인만 나누고, 작성자 확인은 Service에서 한다. 확인 순서는 401 → 404 → 403이다.
- 모집글·댓글 목록과 모집글 상세에서 작성자를 fetch join으로 함께 가져와 N+1을 막는다.
- 모집글을 삭제할 때 `deleteAllByStudyPostId`(JPQL bulk delete)로 댓글을 먼저 지운다.
- 상세, 댓글 목록은 공개 API지만 토큰이 있으면 `isAuthor`를 판단해 프론트에서 수정/삭제 버튼 노출에 쓴다.

## 검증
- 서비스 단위 테스트: `AuthServiceTest` 7개, `StudyPostServiceTest` 6개, `CommentServiceTest` 9개
    - `./backend/gradlew -p backend test` → BUILD SUCCESSFUL (2026-11-16 실행)
- Playwright E2E: `frontend/e2e/auth.spec.ts` 5개, `study-post.spec.ts` 3개, `comment.spec.ts` 3개 (인증, 모집글, 댓글 흐름)
    - `npx playwright test` → 11 passed (3회 연속 통과)
- Swagger로 직접 확인: API 1~13번 (권한, 에러 케이스 포함)
- 프론트: `npm run build`, `npm run lint` 통과
- 브라우저에서 직접 확인: 작성자에게만 수정·삭제 버튼 노출, 모바일 화면

## 남은 이슈
- `TokenBlacklist`가 인메모리라 서버 재시작 시 로그아웃한 토큰이 다시 유효해진다. 서버가 여러 대면 공유되지 않는다.
