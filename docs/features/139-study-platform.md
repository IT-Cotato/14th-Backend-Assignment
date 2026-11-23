# 코테이토 스터디 매칭 플랫폼

- 이슈: #139
- PR: (TODO: 확인 필요)
- 작성일: 2026-11-21

## 배경
코테이토의 스터디 신청은 네이버 카페에서 이뤄지고 있어서, 이를 대신할 전용 스터디 매칭 플랫폼의 핵심 기능을 만든다.
별도의 가입 신청 기능은 없고 **모집글에 댓글을 남기는 것이 곧 스터디 신청**이다.
5,6주차 과제 명세에 따라 백엔드(`backend/`)와 프론트엔드(`frontend/`)를 한 레포(모노레포)에서 함께 개발한다.

권한은 세 가지로 나눈다.
- 공개: 회원가입, 로그인, 모집글 목록과 상세, 댓글 목록
- 로그인: 로그아웃, 내 정보, 모집글 작성, 댓글 작성
- 작성자: 모집글과 댓글의 수정, 삭제 (로그인 + 본인)

## 구현 내용

### 공통 (선행 작업)
- 루트의 Gradle 프로젝트를 `backend/` 로 옮기고, 과제 스택에 맞춰 Spring Boot 3.5 / Java 17 로 내림
- 1-3주차 예제(`book` 도메인, `HelloController`) 제거
- `ErrorCode` 를 과제 명세의 상수 이름(`INVALID_INPUT`, `NOT_POST_AUTHOR` 등)으로 정리
- `GlobalExceptionHandler`
  - `@Valid` 실패는 첫 필드의 메시지를 `message` 로 내려줌
  - 잘못된 JSON, enum, 파라미터 타입은 400 `INVALID_INPUT`
  - 없는 주소는 404 `RESOURCE_NOT_FOUND`, 지원하지 않는 메서드는 405 `METHOD_NOT_ALLOWED` + `Allow` 헤더 (이전에는 500)
- `ResponseUtils.noContent()`(204), 목록 응답용 `PageResponse` 추가

### 1. 인증 (`auth`, `user`)
- 엔티티: `User`(email, password, name, nickname, generation, part), `LogoutToken`(token, expiresAt)
- 흐름
  - `AuthController` -> `AuthService` -> `UserRepository`, `LogoutTokenRepository`
  - `UserController` -> `UserService` -> `UserRepository`
- 회원가입: 이메일 중복 확인 후 BCrypt 로 암호화해서 저장
- 로그인: 이메일과 비밀번호 확인 후 `JwtProvider` 가 jjwt 로 access token 발급
- 로그아웃: 사용한 토큰을 만료 시각과 함께 `logout_token` 에 저장 (중복 저장 방지)
- Security
  - `JwtAuthenticationFilter`: 토큰이 유효하고 `logout_token` 에 없으면 userId 를 인증 정보로 넣고, 아니면 익명으로 통과
  - `JwtAuthenticationEntryPoint`: 인증이 필요한 경로에 익명으로 오면 401 `ErrorResponse`
  - `SecurityConfig`: STATELESS, 공개 경로는 `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/study-posts/**`
- Swagger 에 Bearer 인증 버튼 추가

### 2. 스터디 모집글 (`studypost`)
- 엔티티: `StudyPost`(user, title, content, studyStatus), `StudyStatus`(`RECRUITING`, `CLOSED`)
- 흐름: `StudyPostController` -> `StudyPostService` -> `StudyPostRepository`, `StudyPostCommentRepository`
- 작성: 처음 상태는 `RECRUITING`
- 목록: `StudyPostRepository.search` 가 keyword(제목 또는 내용), status, part(작성자 파트)를 값이 있을 때만 조건으로 걸고 최신순으로 페이징. 작성자는 `join fetch`, 댓글 수는 페이지의 글 id 로 한 번에 셈
- 상세: `findWithUserById` 로 작성자와 함께 읽고, 로그인한 사용자가 작성자면 `isAuthor = true`
- 수정: 보낸 필드만 바꾸고, 모집 마감(`studyStatus`)도 수정 API 로 처리
- 삭제: 작성자 확인 후 댓글을 먼저 일괄 삭제하고 글 삭제

### 3. 댓글 = 스터디 신청 (`comment`)
- 엔티티: `StudyPostComment`(studyPost, user, content)
- 흐름: `CommentController` -> `CommentService` -> `StudyPostCommentRepository`, `StudyPostRepository`
- 작성: 모집이 마감된 글이면 409 `STUDY_POST_CLOSED`
- 목록: 오래된 순, 작성자는 `join fetch`, 댓글마다 `isAuthor`
- 수정, 삭제: 댓글 작성자만 (아니면 403 `NOT_COMMENT_AUTHOR`)

### 4. 프론트엔드 (`frontend/`)
- Vite + React + TypeScript + Tailwind CSS, React Router, axios, TanStack Query. 개발 서버는 3000번 포트 고정
- 화면 6개: 회원가입(`/signup`), 로그인(`/login`), 마이페이지(`/mypage`), 목록(`/studies`), 상세(`/studies/:id`, 댓글 섹션 포함), 작성/수정(`/studies/new`, `/studies/:id/edit`, `StudyFormPage` 하나로)
- `api/client.ts`: axios 인스턴스 하나에서 Bearer 헤더, 응답 `data` 언래핑, 401 처리(토큰 삭제 후 로그인 화면 이동)
- `auth/`: 로그인 상태(`AuthContext`, `AuthProvider`), 로그인 필요 화면 보호(`ProtectedRoute`)
- 로그인 상태에 따라 달라지는 헤더(닉네임, 로그아웃), 서버 에러 `message` 를 화면에 그대로 표시
- 목록의 검색 조건(keyword, status, part, page)은 주소의 쿼리스트링에 둠

### 5. E2E (`frontend/e2e/`)
- Playwright 로 인증(6개), 모집글(5개), 댓글(4개) 흐름의 정상, 에러 시나리오 15개
- 화면에서 숨긴 동작(남의 글, 댓글 수정과 삭제, 마감된 글에 댓글)은 API 를 직접 불러 401, 403, 409 도 확인
- 같은 DB 를 쓰므로 `workers: 1`, 계정과 글 제목은 매번 UUID 로 새로 만들고 비밀번호도 실행할 때 랜덤으로 만듦

## API 명세

자세한 요청/응답 예시와 검증 메시지는 `docs/api-spec.md` 에 있다.

### 인증

| 메서드 | URL | 요청 | 응답 | 에러 케이스 |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/signup` | `SignupRequest` (email, password, name, nickname, generation, part) | 201 `SignupResponse` (userId) | 400 `INVALID_INPUT`, 409 `EMAIL_ALREADY_EXISTS` |
| POST | `/api/auth/login` | `LoginRequest` (email, password) | 200 `TokenResponse` (accessToken, tokenType) | 400 `INVALID_INPUT`, 401 `INVALID_CREDENTIALS` |
| POST | `/api/auth/logout` | Bearer 토큰 | 204 | 401 `UNAUTHORIZED` |
| GET | `/api/users/me` | Bearer 토큰 | 200 `UserMeResponse` (userId, email, name, nickname, generation, part) | 401 `UNAUTHORIZED` |

### 스터디 모집글

| 메서드 | URL | 요청 | 응답 | 에러 케이스 |
| --- | --- | --- | --- | --- |
| POST | `/api/study-posts` | `StudyPostCreateRequest` (title, content) | 201 `StudyPostIdResponse` (postId) | 400 `INVALID_INPUT`, 401 `UNAUTHORIZED` |
| GET | `/api/study-posts` | query: keyword, status, part, page, size | 200 `PageResponse<StudyPostListResponse>` | 400 `INVALID_INPUT` |
| GET | `/api/study-posts/{postId}` | - | 200 `StudyPostDetailResponse` (isAuthor 포함) | 404 `STUDY_POST_NOT_FOUND` |
| PATCH | `/api/study-posts/{postId}` | `StudyPostUpdateRequest` (title, content, studyStatus 중 보낸 것만) | 200 `StudyPostIdResponse` | 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 403 `NOT_POST_AUTHOR`, 404 `STUDY_POST_NOT_FOUND` |
| DELETE | `/api/study-posts/{postId}` | - | 204 | 401 `UNAUTHORIZED`, 403 `NOT_POST_AUTHOR`, 404 `STUDY_POST_NOT_FOUND` |

### 댓글 (스터디 신청)

| 메서드 | URL | 요청 | 응답 | 에러 케이스 |
| --- | --- | --- | --- | --- |
| POST | `/api/study-posts/{postId}/comments` | `CommentRequest` (content) | 201 `CommentIdResponse` (commentId) | 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 404 `STUDY_POST_NOT_FOUND`, 409 `STUDY_POST_CLOSED` |
| GET | `/api/study-posts/{postId}/comments` | query: page, size | 200 `PageResponse<CommentResponse>` (isAuthor 포함) | 404 `STUDY_POST_NOT_FOUND` |
| PATCH | `/api/comments/{commentId}` | `CommentRequest` (content) | 200 `CommentIdResponse` | 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 403 `NOT_COMMENT_AUTHOR`, 404 `COMMENT_NOT_FOUND` |
| DELETE | `/api/comments/{commentId}` | - | 204 | 401 `UNAUTHORIZED`, 403 `NOT_COMMENT_AUTHOR`, 404 `COMMENT_NOT_FOUND` |

공통으로 없는 주소는 404 `RESOURCE_NOT_FOUND`, 지원하지 않는 메서드는 405 `METHOD_NOT_ALLOWED` 다. 인증이 필요한 경로는 Security 가 먼저 검사해서 토큰이 없으면 401 이 먼저 나온다.

## 설계 결정

### 인증
- **로그아웃 토큰을 DB 에 저장**: JWT 는 서버에 상태가 없어 로그아웃해도 만료 전까지 쓸 수 있다. 로그아웃한 토큰을 `logout_token` 에 만료 시각과 함께 저장하고 필터에서 확인해 막는다.
- **필터는 무효 토큰도 익명으로 통과**: 토큰이 없거나 유효하지 않아도 필터에서 바로 401 을 내지 않는다. 그래야 공개 조회 API(상세, 댓글 목록)에서도 로그인 여부를 알아 `isAuthor` 를 계산할 수 있다. 401 은 인증이 필요한 경로에서 `JwtAuthenticationEntryPoint` 가 내린다.
- **테이블 이름 `users`**: `user` 는 MySQL 예약어라서 바꿨다.
- **로그인 실패를 구분하지 않음**: 이메일이 없는 경우와 비밀번호가 틀린 경우 모두 401 `INVALID_CREDENTIALS` 로 같은 메시지를 내린다.

### 스터디 모집글
- **목록 쿼리 수 고정**: 작성자는 `join fetch` 로 같이 읽고, 댓글 수는 페이지에 담긴 글 id 로 한 번에 세어 글 수만큼 쿼리가 늘지 않게 했다.
- **모집 마감은 수정 API 로**: 별도의 마감 API 를 만들지 않고 `PATCH` 의 `studyStatus` 로 `RECRUITING` 과 `CLOSED` 를 자유롭게 바꾼다.
- **삭제 시 댓글을 서비스에서 먼저 삭제**: DB cascade 가 아니라 애플리케이션에서 댓글을 일괄 삭제한 뒤 글을 지운다.
- **`isAuthor` getter 이름을 직접 정함**: Lombok 이 boolean `isAuthor` 의 getter 를 `isAuthor()` 로 만들면 Jackson 이 이를 `author` 프로퍼티로 읽어 작성자 객체 `author` 와 겹치고 덮어썼다. getter 를 `getIsAuthor()` 로 정해 해결하고 `.dev/learnings/003-is-prefix-boolean-json-collision.md` 에 기록했다.

### 댓글
- **같은 사람이 여러 번 댓글 가능**: 명세가 댓글을 곧 신청으로 보므로 질문이나 추가 대화도 댓글로 할 수 있게 `(study_post_id, user_id)` 에 UNIQUE 를 걸지 않았다.
- **마감된 글은 409**: 모집이 마감된 글에는 댓글(신청)을 쓸 수 없게 409 `STUDY_POST_CLOSED` 로 막는다.

### 프론트엔드
- **API 호출을 axios 인스턴스 하나로**: 토큰 헤더, `data` 언래핑, 401 처리를 한 곳에서 해서 화면마다 반복하지 않는다.
- **검색 조건은 쿼리스트링에**: 주소 반영이 늦게 커밋되는 사이 입력한 값이 지워지지 않게, 검색 폼은 직접 제출한 조건이 아닐 때만 주소 값에 맞춘다.
- **접근성 있는 마크업**: 입력에는 `label`, 동작은 `button`, 이동은 링크로 만들어 E2E 가 `getByRole`, `getByLabel` 로 요소를 찾을 수 있게 했다.

## 검증
- `backend/` 에서 `./gradlew clean test`: 33개 통과, 실패 0 (`AuthServiceTest` 7, `StudyPostServiceTest` 9, `CommentServiceTest` 9, `JwtProviderTest` 3, `StudyPostResponseJsonTest` 2, `GlobalExceptionHandlerTest` 2, `Backend14thApplicationTests` 1)
- `frontend/` 에서 `npm run build` 성공, `npm run lint`(oxlint) 통과
- `frontend/` 에서 `npx playwright test`: 15 passed (백엔드와 MySQL 을 켠 상태)

| 기능 | E2E 시나리오 | 결과 |
| --- | --- | --- |
| 인증 | 기수, 파트 입력 후 회원가입 -> 로그인 -> 헤더 닉네임 -> 마이페이지 기수, 파트 확인 -> 로그아웃 | 통과 |
| 인증 | 중복 이메일, 기수 0 입력, 틀린 비밀번호 -> 에러 메시지 | 통과 |
| 인증 | 로그인 없이 마이페이지 접근 -> 로그인 화면 이동, 로그아웃한 토큰으로 요청 -> 401 | 통과 |
| 모집글 | 작성 -> 목록 검색, 필터 -> 상세 -> 수정 -> 모집 마감 -> 삭제 | 통과 |
| 모집글 | 로그인 없이 작성 시도(401), 남의 글 수정, 마감, 삭제 불가(403), 제목 누락, 잘못된 상태나 파트 값(400) | 통과 |
| 댓글 | 작성 -> 수정 -> 삭제 | 통과 |
| 댓글 | 마감된 글에 작성 불가(409), 남의 댓글 수정, 삭제 불가(403), 비로그인 작성 불가(401) | 통과 |

- Swagger UI 에서 13개 API 직접 호출: (TODO: 확인 필요)

## 남은 이슈
- 만료 시각이 지난 `logout_token` 행을 정리하지 않는다. 만료된 토큰은 JWT 검증에서 이미 거부되어 동작에는 영향이 없다. 필요해지면 `@Scheduled` 로 주기적으로 지우거나 Redis TTL 로 옮긴다.
- refresh token 이 없어서 access token 이 만료되면 다시 로그인해야 한다.
- E2E 가 개발 DB 에 `e2e-` 로 시작하는 테스트 데이터를 남긴다.
