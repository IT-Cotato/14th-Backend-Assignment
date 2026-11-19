# 코테이토 스터디 매칭 플랫폼

> 파일 이름은 이슈 번호가 생기면 `docs/features/{이슈번호}-study-platform.md`로 바꾼다.

## 개요

코테이토 회원이 스터디 모집글을 올리고, 다른 회원이 **댓글로 스터디를 신청**하는 플랫폼.
백엔드(Spring Boot)와 프론트엔드(React + Vite)를 모노레포(`backend/`, `frontend/`)로 구성했다.

- API 명세: [`docs/api-spec.md`](../api-spec.md)
- ERD: [`docs/erd/README.md`](../erd/README.md)

---

## 1. 인증

### 구현 내용

| API | 설명 |
|---|---|
| `POST /api/auth/signup` | 이메일·비밀번호·이름·닉네임·기수·파트로 가입. 비밀번호는 BCrypt |
| `POST /api/auth/login` | JWT `accessToken` 발급, `tokenType: "Bearer"` |
| `POST /api/auth/logout` | 사용한 토큰을 `blacklisted_token`에 만료 시각과 함께 저장 → 이후 401 |
| `GET /api/users/me` | 내 정보 (비밀번호 제외) |

### 설계 포인트

- **JwtAuthenticationFilter**: 유효하고 블랙리스트에 없는 토큰이면 `userId`를 principal로 넣는다. 토큰이 없거나 잘못됐으면 그냥 통과시킨다.
  - 공개 API(목록·상세·댓글 목록)는 비로그인으로 동작하고, 로그인 API는 Security가 401을 낸다.
  - 컨트롤러는 `@AuthenticationPrincipal Long userId`로 받는다. 비로그인이면 `null` → `isAuthor=false`.
- **SecurityErrorHandler**: 필터 단계의 401/403을 `HandlerExceptionResolver`로 넘겨서 `GlobalExceptionHandler`가 처리한다. 덕분에 인증 에러도 다른 API와 같은 `ErrorResponse` 형식이다.
- **로그아웃 저장 방식**: DB 테이블 `blacklisted_token`(token UK, expires_at). 서버를 재시작해도 로그아웃이 유지된다.
- **검증**: Bean Validation 메시지를 `INVALID_INPUT`의 `message`로 내려준다 (예: "기수는 1 이상이어야 해요."). Enum에 없는 `part`는 `HttpMessageNotReadableException` → 400.
- 같은 초에 두 번 로그인해도 토큰이 겹치지 않게 `jti`(UUID)를 넣었다.

### 프론트

- `/signup`: 6개 입력 + 파트 선택 목록, 클라이언트 검증 메시지 + 서버 에러 메시지
- `/login`: 성공 시 토큰을 `localStorage`에 저장하고 `/studies`로 이동
- `/mypage`: `RequireAuth`로 보호, 이메일·이름·닉네임·기수·파트 표시
- 헤더: 로그인 상태면 `닉네임님 / 마이페이지 / 로그아웃`, 아니면 `로그인 / 회원가입`
- axios 인터셉터: 토큰을 보냈는데 401이면 토큰을 지우고 `/login`으로 이동

---

## 2. 스터디 모집글

### 구현 내용

| API | 설명 |
|---|---|
| `POST /api/study-posts` | 작성. 처음 상태는 `RECRUITING` |
| `GET /api/study-posts` | `keyword`(제목·내용) + `status` + `part`(작성자 파트) AND 필터, 최신순 페이징 |
| `GET /api/study-posts/{id}` | 상세, `isAuthor` |
| `PATCH /api/study-posts/{id}` | 작성자만. 보낸 필드만 수정 (모집 상태 포함) |
| `DELETE /api/study-posts/{id}` | 작성자만. 댓글 bulk delete 후 글 삭제 |

### 설계 포인트

- 목록 쿼리는 JPQL `join fetch p.user`로 작성자를 함께 가져오고, 파트 필터도 이 조인으로 건다. 페이징을 위해 `countQuery`를 따로 둔다.
- `commentCount`는 페이지에 나온 글 id로 `group by` 한 번에 센다 (N+1 방지).
- 정렬은 `createdAt desc, id desc` (같은 시각에 만든 글도 순서 고정).
- 잘못된 `status`/`part` 쿼리 값은 `MethodArgumentTypeMismatchException` → 400 `INVALID_INPUT`.
- 수정 DTO는 모든 필드가 선택이지만, 보낸 필드가 공백이면 400.

### 프론트

- `/studies`: 검색어·모집 상태·파트 필터(URL 쿼리에 저장), 이전/다음 페이지, 작성자 닉네임·기수·파트 표시
- `/studies/:id`: 작성자에게만 `모집 마감(재개)`·`수정`·`삭제` 버튼
- `/studies/new`, `/studies/:id/edit`: 수정 화면에서 모집 상태 선택 가능

---

## 3. 댓글 (= 스터디 신청)

### 구현 내용

| API | 설명 |
|---|---|
| `POST /api/study-posts/{id}/comments` | 작성. `CLOSED` 글이면 409 `STUDY_POST_CLOSED` |
| `GET /api/study-posts/{id}/comments` | 오래된 순 페이징, `isAuthor` |
| `PATCH /api/comments/{id}` | 작성자만 |
| `DELETE /api/comments/{id}` | 작성자만 |

### 프론트

- 상세 화면 하단에 댓글 목록, 작성 폼(마감 글이면 안내 문구), 내 댓글에만 `댓글 수정`·`댓글 삭제`

---

## 테이블 변경

명세 최소 구성에서 추가한 것:

- `blacklisted_token` 테이블 (로그아웃 토큰)
- 테이블 이름 `users` (MySQL 예약어 `user` 회피, 엔티티는 `User`)

## 테스트

- 백엔드 단위 테스트(Mockito): `AuthServiceTest`, `StudyPostServiceTest`, `CommentServiceTest` + `contextLoads` → `./gradlew test` 25 passed
- E2E(Playwright): `frontend/e2e/` 13개 시나리오 → `npx playwright test` 13 passed

| 기능 | 정상 | 에러 |
|---|---|---|
| 인증 | 가입(기수·파트) → 로그인 → 헤더 닉네임 → 마이페이지 → 로그아웃 | 중복 이메일, 기수 0, 틀린 비밀번호, 비로그인 마이페이지, 로그아웃 토큰 재사용 401 |
| 모집글 | 작성 → 목록 → 검색·상태·파트 필터 → 상세 → 수정 → 마감 → 삭제 | 비로그인 작성(화면·API 401), 남의 글 수정·삭제(버튼 없음·API 403), 없는 글 |
| 댓글 | 작성 → 수정 → 삭제 | 마감 글 작성(입력창 없음·API 409), 남의 댓글 수정·삭제(버튼 없음·API 403) |

## 하네스 변경

- `CLAUDE.md`: 도메인(User/StudyPost/StudyPostComment, RECRUITING/CLOSED), 모노레포 구조, 프론트·E2E 명령어
- `.claude/rules/frontend-conventions.md`: axios 인스턴스만 사용, `ApiResponse.data` 꺼내기, label·button·Link 규칙, 401 처리
- `.claude/rules/e2e-conventions.md`: 역할·라벨 선택자, 고정 대기 금지, 테스트 데이터 독립(`e2e-` 이메일), 비밀번호 하드코딩 금지
- `.claude/skills/run-e2e`: 백엔드 기동 → E2E 실행 → 실패 원인 분류 절차
- hook `protect-secrets.sh`, `settings.json`: 모노레포 경로(backend/, frontend/)의 환경 변수 파일까지 보호
