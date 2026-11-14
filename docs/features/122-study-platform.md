# 5,6주차 코테이토 스터디 매칭 플랫폼

## 목표

[API 명세](../api-spec.md)의 13개 API와 6개 화면을 구현합니다. 회원가입(기수, 파트), 로그인/로그아웃, 내 정보 조회, 스터디 모집글 CRUD(검색, 필터, 페이징), 댓글(= 스터디 신청) CRUD가 대상입니다. 핵심 사용자 흐름은 Playwright E2E 테스트로 검증합니다.

- 백엔드: `backend/` (Spring Boot 4.1.1, Controller → Service → Repository)
- 프론트: `frontend/` (Vite, React, TypeScript, Tailwind CSS, React Router, axios)

## 변경 사항

### 1. 인증 (API 1~4)

| 커밋 | 내용 |
|---|---|
| `fcc02d1` | JWT 기반 회원가입, 로그인, 로그아웃, 내 정보 조회 |
| `bfe4016` | 인증 서비스 단위 테스트 |
| `3145136` | 프론트 API 클라이언트와 인증 상태 관리 |
| `27831ff` | 회원가입, 로그인, 마이페이지 화면과 헤더 |

**백엔드**

- `domain/auth`(`AuthController`, `AuthService`), `domain/user`(`User`, `Part`, `UserController`, `UserService`)
- 비밀번호는 BCrypt로 저장합니다. 로그인 실패는 이메일이 없을 때와 비밀번호가 틀릴 때 모두 `INVALID_CREDENTIALS`(401)입니다.
- `global/security`
  - `JwtTokenProvider`: 토큰의 subject는 userId입니다.
  - `JwtAuthenticationFilter`: 유효한 토큰이면 principal에 userId(Long)를 넣고, 토큰이 없거나 무효이면 익명으로 넘깁니다.
  - `JwtAuthenticationEntryPoint`: 필터 단계의 401을 `ErrorResponse` 형식으로 내려줍니다.
  - `TokenBlacklist`: 로그아웃한 토큰을 만료 시각까지 인메모리로 보관합니다.
- `SecurityConfig`: 회원가입, 로그인, 모집글 목록·상세, 댓글 목록은 공개이고, 나머지 `/api`는 로그인이 필요합니다.
- `GlobalExceptionHandler`: `@Valid` 실패, 읽을 수 없는 본문, 잘못된 타입의 파라미터를 `INVALID_INPUT`(400)으로 처리합니다.

**프론트**

- `api/client.ts`: axios 인스턴스입니다. 요청에 `Authorization: Bearer` 헤더를 붙이고, 로그인·회원가입이 아닌 요청에서 401이 오면 토큰을 지우고 `/login`으로 보냅니다.
- `auth/AuthProvider.tsx`, `auth/RequireAuth.tsx`, `auth/tokenStorage.ts`: 토큰은 `localStorage`에 저장하고, 로그인이 필요한 화면을 보호합니다.
- `pages/SignupPage.tsx`(기수, 파트 입력과 검증), `pages/LoginPage.tsx`, `pages/MyPage.tsx`, `components/Header.tsx`

### 2. 스터디 모집글 (API 5~9)

| 커밋 | 내용 |
|---|---|
| `66ba4c1` | 모집글 API, `StudyPost`·`StudyPostComment` 엔티티, ErrorCode 추가 |
| `1b98360` | 모집글 서비스 단위 테스트와 검색 쿼리 테스트 |
| `05f0c75` | 스터디 목록, 상세, 작성, 수정 화면 |

**백엔드** (`domain/studypost`)

- `StudyPost`: `user_id`(LAZY, 단방향), `title`, `content`, `study_status`(문자열 Enum `StudyStatus`: RECRUITING, CLOSED). 처음 만들면 RECRUITING입니다.
- 목록 `StudyPostRepository.search`
  - `join fetch p.user`로 작성자를 함께 가져옵니다.
  - keyword(제목 또는 내용), status, 작성자 part 조건은 값이 있을 때만 적용하고, 모두 만족하는 글만 나옵니다.
  - `createdAt desc, id desc`로 정렬합니다.
- 댓글 수는 현재 페이지의 글 id로 `StudyPostCommentRepository.countByPostIds`(group by)를 한 번 실행해서 셉니다. 목록 조회는 쿼리 3번(목록, 전체 개수, 댓글 수)이고, 글 수가 늘어도 쿼리 수는 그대로입니다.
- 상세의 `isAuthor`는 `@AuthenticationPrincipal Long userId`로 판단합니다. 공개 API여도 필터가 유효한 토큰의 사용자를 넣어 주고, 익명이면 null이라 false가 됩니다.
- 수정(PATCH)은 null이 아닌 필드만 바꿉니다. 공백만 있는 제목·내용은 400입니다.
- 삭제는 `deleteAllByPostId`(JPQL 벌크 삭제)로 댓글을 먼저 지우고 글을 지웁니다. cascade는 쓰지 않습니다.
- 작성자 확인(403)과 없는 글(404)은 service의 `findOwnedPost`에서 처리합니다.
- `ErrorCode`에 `STUDY-001~003`, `COMMENT-001~002`를 명세 8장 그대로 추가했습니다.
- 쿼리 값 검증(`page` 0 이상, `size` 1~100) 실패인 `HandlerMethodValidationException`도 400으로 처리합니다.
- 공통 응답: `PageResponse`(content, page, size, totalElements, totalPages), `AuthorResponse`(nickname, generation, part)

**프론트**

- `pages/StudiesPage.tsx`: 검색어, 모집 상태, 파트 필터와 페이지를 주소 쿼리(`?keyword=&status=&part=&page=`)에 둡니다. 각 글에 닉네임, 기수, 파트, 댓글 수를 표시합니다.
- `pages/StudyDetailPage.tsx`: 작성자에게만 수정(링크), 모집 마감, 삭제 버튼을 보여줍니다. 삭제는 화면 안에서 한 번 더 확인합니다.
- `pages/StudyFormPage.tsx`: 작성과 수정을 함께 처리합니다. 수정 화면에서는 모집 상태도 바꿀 수 있고, 작성자가 아니면 폼을 보여주지 않습니다.
- `/studies/new`, `/studies/:id/edit`는 `RequireAuth`로 보호합니다.
- 공통 컴포넌트: `Pagination`(링크로 이동), `StatusBadge`, `LoadingStatus`, `format.ts`

### 3. 댓글 = 스터디 신청 (API 10~13)

| 커밋 | 내용 |
|---|---|
| `8634753` | 댓글 API |
| `f12d77e` | 댓글 서비스 단위 테스트 |
| `d33c4be` | 모집글 상세 안의 댓글 목록, 작성, 수정, 삭제 화면 |

**백엔드** (`domain/comment`)

- `StudyPostComment`: `study_post_id`, `user_id`(모두 LAZY, 단방향), `content`
- 작성: 마감된 글이면 `STUDY_POST_CLOSED`(409)입니다. 같은 사람이 여러 번 달 수 있습니다.
- 목록: 오래된 순(`createdAt asc, id asc`)으로 페이징하고, 작성자를 `join fetch`로 함께 가져옵니다. 없는 글이면 404입니다.
- 수정, 삭제: 작성자가 아니면 `NOT_COMMENT_AUTHOR`(403), 없으면 `COMMENT_NOT_FOUND`(404)입니다.

**프론트**

- `components/CommentSection.tsx`: 목록, 작성, 페이지 이동(`?commentPage=`)을 맡습니다. 로그인하지 않았으면 작성 폼 대신 로그인 링크를 보여줍니다. 마감된 글에 쓰면 서버 메시지를 그대로 보여줍니다.
- `components/CommentItem.tsx`: 내 댓글에만 수정(인라인), 삭제(확인 후) 버튼을 보여줍니다.

### 4. E2E 테스트 (`e1d25bf`)

`frontend/e2e/`에 있습니다.

| 파일 | 시나리오 |
|---|---|
| `auth.spec.ts` | 회원가입 → 로그인 → 헤더 닉네임 → 마이페이지 기수·파트 → 로그아웃 / 중복 이메일 / 기수 0(화면 + API 400) / 틀린 비밀번호 / 비로그인 마이페이지(화면 + API 401) |
| `study-posts.spec.ts` | 작성 → 목록 → 검색, 상태·파트 필터 조합 → 상세 → 수정 → 모집 마감 → 삭제(API 404 확인) / 페이지 이동 / 잘못된 status·part 400 / 비로그인 작성(화면 + API 401) / 남의 글(버튼 없음, 수정 화면 차단, API 403) |
| `comments.spec.ts` | 작성 → 수정 → 삭제 / 마감된 글(화면 메시지 + API 409) / 남의 댓글(버튼 없음, API 403) |

- 요소는 `getByRole`, `getByLabel`로 찾고, 고정 대기 없이 `expect`가 조건을 기다리게 합니다.
- 테스트마다 `e2e-<UUID>@example.com` 계정과 고유 제목을 만듭니다. 비밀번호는 실행할 때 `randomUUID()`로 만듭니다.
- 백엔드 주소는 기본 `http://localhost:8081`이고, `E2E_API_URL`로 바꿀 수 있습니다.

## 검증

| 항목 | 명령 | 결과 |
|---|---|---|
| 백엔드 테스트 | `backend/`에서 `.\gradlew.bat test` | 38개 통과 |
| 프론트 빌드 | `frontend/`에서 `npm run build` | 성공 (`tsc -b`가 `e2e/`도 타입 검사) |
| 프론트 lint | `frontend/`에서 `npm run lint` | 경고 0 |
| E2E | `frontend/`에서 `npx playwright test` | 13개 통과, `--repeat-each=3`으로 39개 통과 |
| Swagger | `/v3/api-docs` | `/api` 아래 13개 API 확인 |

백엔드 테스트 구성은 다음과 같습니다.

| 테스트 | 개수 | 내용 |
|---|---|---|
| `StudyPostServiceTest` | 11 | 작성, 필터 전달, 댓글 수 집계, isAuthor, 수정·삭제 403/404 |
| `StudyPostRepositoryTest` | 6 | 실제 MySQL에서 키워드·상태·파트 조합, 최신순, 페이징, 댓글 벌크 삭제 (트랜잭션 롤백) |
| `StudyPostCommentServiceTest` | 11 | 작성, 마감된 글 409, isAuthor, 수정·삭제 403/404 |
| `AuthServiceTest`, `LogoutTest`, `TokenBlacklistTest` | 9 | 인증 |
| `Backend14thApplicationTests` | 1 | 컨텍스트 로딩 |

## ERD/문서 변경

- 테이블과 컬럼 추가는 없습니다. `study_post`, `study_post_comment`는 명세 2장의 테이블입니다.
- [API 명세](../api-spec.md)에 "구현 메모"를 추가했습니다. 명세에 없던 세부 규칙(페이지 값 범위, 댓글 목록 기본 크기, 날짜의 소수점 초 등)을 적었습니다.

## 한계와 미확인 사항

- 테스트가 실제 로컬 DB를 씁니다. `StudyPostRepositoryTest`는 롤백되지만, E2E는 `e2e-` 계정과 글, 댓글을 로컬 DB에 남깁니다.
- 로그아웃 목록은 인메모리라서 서버를 재시작하면 로그아웃한 토큰이 만료 전까지 다시 유효해집니다.
- 목록 검색은 `like '%keyword%'`라서 데이터가 많아지면 느려질 수 있고, 검색어의 `%`, `_`는 와일드카드로 동작합니다.
- 모바일 화면은 Tailwind 반응형 클래스로 작성했지만, 모바일 뷰포트 E2E는 돌리지 않았습니다.
- E2E는 Chromium에서만 실행했습니다.

## 남은 과제

- ERD 갱신 (사용자가 직접)
- 테스트 DB 분리, E2E 데이터 정리 방법
- `COMMON_BAD_REQUEST`(`COMMON-001`) 삭제 여부 결정 (명세상 "지울 후보")
