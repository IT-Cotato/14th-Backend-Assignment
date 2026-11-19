# 코테이토 스터디 매칭 플랫폼 (인증, 모집글, 댓글)

- 이슈: #133
- PR: (PR 생성 후 추가)
- 작성일: 2026-11-19

## 배경
5,6주차 과제다. 코테이토 부원이 스터디 모집글을 올리고, 다른 부원이 댓글로 신청하는 서비스를 백엔드와 프론트엔드 모두 만든다.
한 레포에서 백엔드(`backend/`)와 프론트엔드(`frontend/`)를 함께 관리하고, 워크북 순서에 따라 **인증 → 모집글 → 댓글**을 순서마다 백엔드와 화면을 같이 만들었다. 마지막으로 E2E 테스트와 서비스 단위 테스트를 추가했다.

부원 정보에는 코테이토만의 특징인 **기수**와 **파트**(PM, DE, BE, FE)를 넣었다. 모집글 목록은 작성자 파트로 거를 수 있고, 모집이 마감된 글에는 신청(댓글)을 받지 않는다.

## 구현 내용

### 1. 인증

**백엔드** (`domain/auth`, `domain/user`, `global/security`)

| 클래스 | 역할 |
|---|---|
| `AuthService` | 회원가입(이메일 중복 검사, BCrypt 암호화), 로그인(JWT 발급), 로그아웃(토큰을 블랙리스트에 저장) |
| `UserService` | 토큰의 사용자 id로 내 정보 조회 |
| `JwtProvider` | HS256 토큰 생성·검증, `Bearer ` 헤더에서 토큰 꺼내기, 사용자 id와 만료 시각 읽기. 비밀키와 만료 시간은 `.env`의 `JWT_SECRET`, `JWT_EXPIRATION_MS`에서 읽는다 |
| `JwtAuthenticationFilter` | 요청마다 토큰을 확인한다. 유효하고 블랙리스트에 없으면 사용자 id를 인증 정보로 넣는다. 토큰이 없거나 무효면 막지 않고 그대로 통과시킨다 |
| `JwtAuthenticationEntryPoint` | 로그인이 필요한 API에 인증 없이 들어오면 `GlobalExceptionHandler`로 넘겨서 `401 COMMON-004`를 공통 에러 형식으로 내려준다 |
| `SecurityConfig` | 로그인이 필요한 경로(작성·수정·삭제, 로그아웃, 내 정보)만 막고 나머지 조회는 공개한다 |
| `TokenBlacklist` | 로그아웃한 토큰과 원래 만료 시각을 저장하는 엔티티 (`token_blacklist` 테이블) |

흐름: 로그인 → 토큰 발급 → 프론트가 모든 요청에 `Authorization: Bearer` 첨부 → 필터가 사용자 식별 → 컨트롤러는 `@AuthenticationPrincipal Long userId`로 받음 (공개 API에서 로그인하지 않았으면 `null`)

**프론트** (`/signup`, `/login`, `/mypage`)
- `AuthProvider`가 토큰을 `localStorage`에 저장하고, 앱을 시작할 때 `/api/users/me`로 내 정보를 불러온다.
- 헤더는 로그인 상태에 따라 메뉴가 바뀐다. 로그인 전에는 로그인·회원가입, 로그인 후에는 닉네임·마이페이지·로그아웃이 보인다.
- `RequireAuth`가 로그인이 필요한 화면(마이페이지, 글 작성·수정)을 감싼다. 로그인하지 않았으면 `/login`으로 보내고, 로그인한 뒤에는 원래 가려던 화면으로 돌아온다.
- axios 인터셉터가 모든 요청에 토큰을 붙인다. **토큰을 보낸 요청**이 401을 받으면 토큰을 지우고 `/login`으로 보낸다.
- 회원가입 화면은 서버와 같은 기준으로 먼저 검사해서 필드마다 에러 문구를 보여준다. (예: 기수 0 → "기수는 1 이상의 정수여야 해요.")

### 2. 스터디 모집글

**백엔드** (`domain/studypost`)
- `StudyPostService`: 작성, 목록, 상세, 수정, 삭제. 수정과 삭제는 작성자인지 먼저 확인한다.
- `StudyPostRepository.search`: JPQL 한 개로 검색어(제목 또는 내용), 모집 상태, 작성자 파트 조건을 처리한다. 값이 `null`인 조건은 무시한다. 작성자를 fetch join으로 함께 가져오고, 최신순(같은 시각이면 id 내림차순)으로 정렬한다.
- 목록의 댓글 수는 현재 페이지 글들의 id를 모아서 `group by` 쿼리 한 번으로 센다.
- 상세 응답의 `isAuthor`는 요청한 사용자가 작성자인지 서버가 계산한다.
- 수정은 보낸 필드만 바꾼다(`null`은 무시). 모집 상태도 수정 API로 바꾼다.
- 삭제할 때는 댓글을 벌크 delete로 먼저 지우고 글을 지운다.

**프론트** (`/studies`, `/studies/:id`, `/studies/new`, `/studies/:id/edit`)
- 목록: 검색어·모집 상태·파트 검색 폼, 페이지 이동. 검색 조건은 URL 쿼리에 담는다.
- 상세: 작성자에게만 수정·모집 마감(재개)·삭제 버튼이 보인다. 삭제는 화면 안에서 한 번 더 확인한다.
- 작성·수정은 같은 화면을 쓴다. 수정 화면에서만 모집 상태를 고를 수 있고, 작성자가 아니면 "모집글 작성자만 수정할 수 있어요."를 보여준다.

### 3. 댓글 (= 스터디 신청)

**백엔드** (`domain/comment`)
- `CommentService`: 작성, 목록, 수정, 삭제. 마감된 글에 작성하면 `409 STUDY-003`이다.
- 목록은 오래된 순이고, 작성자를 fetch join으로 함께 가져온다. 댓글마다 `isAuthor`를 계산한다.

**프론트** (모집글 상세 아래 `CommentSection`)
- 로그인했고 모집 중인 글이면 신청 메시지 입력창이 보인다. 마감된 글이면 "모집이 마감되어 더 이상 신청할 수 없어요.", 로그인하지 않았으면 로그인 안내가 보인다.
- 내 댓글에만 수정·삭제 버튼이 보인다. 수정은 그 자리에서 입력창으로 바뀐다.

### 4. 테스트
- **E2E** (`frontend/e2e/`, Playwright): 인증 6개, 모집글 4개, 댓글 4개. 정상 흐름과 에러 흐름을 화면으로 확인하고, 화면에서 막는 권한(남의 글·댓글, 마감된 글)은 API를 직접 호출해서 서버도 403·409로 막는지 확인한다.
- **서비스 단위 테스트** (`backend/src/test/`, JUnit + Mockito): `AuthService` 8개, `UserService` 2개, `StudyPostService` 14개, `CommentService` 11개. 스프링과 DB 없이 서비스의 판단 로직만 검사한다.

## API 명세
13개 API를 만들었다. 요청·응답 예시와 검증 규칙의 상세는 `docs/api-spec.md`에 있다.
권한은 공개(토큰 불필요), 로그인(유효한 토큰 필요), 작성자(로그인 + 본인)로 나눈다.

| # | Method | URL | 권한 | 요청 본문 | 성공 응답 | 에러 |
|---|---|---|---|---|---|---|
| 1 | POST | `/api/auth/signup` | 공개 | `email`, `password`(8자 이상), `name`(1~50자), `nickname`(2~10자), `generation`(1 이상), `part` | `201` `{ userId }` | 400 `COMMON-003`, 409 `USER-001` |
| 2 | POST | `/api/auth/login` | 공개 | `email`, `password` | `200` `{ accessToken, tokenType: "Bearer" }` | 401 `USER-002` |
| 3 | POST | `/api/auth/logout` | 로그인 | 없음 | `204` 본문 없음 | 401 `COMMON-004` |
| 4 | GET | `/api/users/me` | 로그인 | 없음 | `200` `{ userId, email, name, nickname, generation, part }` | 401 `COMMON-004` |
| 5 | POST | `/api/study-posts` | 로그인 | `title`(1~50자), `content`(1~2000자) | `201` `{ postId }` | 400 `COMMON-003`, 401 `COMMON-004` |
| 6 | GET | `/api/study-posts?keyword&status&part&page&size` | 공개 | 없음 | `200` 페이지(`postId`, `title`, `studyStatus`, `author`, `commentCount`, `createdAt`) | 400 `COMMON-003` |
| 7 | GET | `/api/study-posts/{postId}` | 공개 | 없음 | `200` 상세(`content`, `isAuthor`, `updatedAt` 포함) | 404 `STUDY-001` |
| 8 | PATCH | `/api/study-posts/{postId}` | 작성자 | `title`, `content`, `studyStatus` 중 바꿀 것만 | `200` `{ postId }` | 400 `COMMON-003`, 401 `COMMON-004`, 403 `STUDY-002`, 404 `STUDY-001` |
| 9 | DELETE | `/api/study-posts/{postId}` | 작성자 | 없음 | `204` 본문 없음 (댓글도 삭제) | 401 `COMMON-004`, 403 `STUDY-002`, 404 `STUDY-001` |
| 10 | POST | `/api/study-posts/{postId}/comments` | 로그인 | `content`(1~500자) | `201` `{ commentId }` | 400 `COMMON-003`, 401 `COMMON-004`, 404 `STUDY-001`, 409 `STUDY-003` |
| 11 | GET | `/api/study-posts/{postId}/comments?page&size` | 공개 | 없음 | `200` 페이지(`commentId`, `content`, `author`, `isAuthor`, `createdAt`) | 400 `COMMON-003`, 404 `STUDY-001` |
| 12 | PATCH | `/api/comments/{commentId}` | 작성자 | `content`(1~500자) | `200` `{ commentId }` | 400 `COMMON-003`, 401 `COMMON-004`, 403 `COMMENT-002`, 404 `COMMENT-001` |
| 13 | DELETE | `/api/comments/{commentId}` | 작성자 | 없음 | `204` 본문 없음 | 401 `COMMON-004`, 403 `COMMENT-002`, 404 `COMMENT-001` |

이번에 추가한 에러 코드 (`ErrorCode`)

| HTTP | 상수 | code | 메시지 |
|---|---|---|---|
| 400 | `INVALID_INPUT` | `COMMON-003` | 입력값이 올바르지 않아요. |
| 401 | `UNAUTHORIZED` | `COMMON-004` | 로그인이 필요해요. |
| 409 | `EMAIL_ALREADY_EXISTS` | `USER-001` | 이미 가입된 이메일이에요. |
| 401 | `INVALID_CREDENTIALS` | `USER-002` | 이메일 또는 비밀번호가 올바르지 않아요. |
| 404 | `STUDY_POST_NOT_FOUND` | `STUDY-001` | 존재하지 않는 스터디 모집글이에요. |
| 403 | `NOT_POST_AUTHOR` | `STUDY-002` | 모집글 작성자만 할 수 있어요. |
| 409 | `STUDY_POST_CLOSED` | `STUDY-003` | 모집이 마감된 글에는 댓글을 쓸 수 없어요. |
| 404 | `COMMENT_NOT_FOUND` | `COMMENT-001` | 존재하지 않는 댓글이에요. |
| 403 | `NOT_COMMENT_AUTHOR` | `COMMENT-002` | 댓글 작성자만 할 수 있어요. |

검증 실패(`@Valid`), 잘못된 JSON, 잘못된 Enum 값과 숫자 형식은 모두 `GlobalExceptionHandler`에서 `400 COMMON-003`으로 바꿔서 내려준다.

## 설계 결정

### 인증
- **로그아웃 처리: DB 블랙리스트 / 메모리 블랙리스트 / 프론트에서만 토큰 삭제**: 명세가 "로그아웃한 토큰은 만료 시각까지 무효"라서 서버가 기억해야 한다. 메모리에 두면 서버를 다시 켤 때 사라져서 `token_blacklist` 테이블을 추가했다. 만료된 행을 지우는 스케줄러는 두지 않았다. 만료된 토큰은 JWT 검증에서 먼저 거절되기 때문이다.
- **refresh token을 둘지**: 명세가 accessToken 하나만 쓰라고 해서 두지 않았다. 만료 시간 기본값은 1시간이다.
- **필터가 직접 401을 내릴지, 통과시킬지**: 필터는 토큰이 없거나 무효여도 통과시키고, 막는 일은 `SecurityConfig`의 경로 설정에 맡겼다. 공개 API(목록, 상세)도 토큰이 있으면 사용자를 알아야 `isAuthor`를 계산할 수 있기 때문이다.
- **401 응답 형식**: 처음에는 EntryPoint에서 JSON을 직접 쓰는 방법을 생각했지만, 그러면 에러 형식을 두 곳에서 관리하게 된다. `HandlerExceptionResolver`로 넘겨서 다른 에러와 똑같이 `GlobalExceptionHandler`가 `ErrorResponse`를 만들게 했다.
- **로그인 실패 메시지**: 없는 이메일과 틀린 비밀번호를 같은 `USER-002`로 내려준다. 따로 알려주면 어떤 이메일이 가입돼 있는지 알아낼 수 있기 때문이다.
- **`auth` 도메인을 따로 둘지, `user`에 넣을지**: 회원가입·로그인·로그아웃은 `TokenBlacklist`까지 다루고 내 정보 조회와 성격이 달라서 `domain/auth`로 나눴다.
- **`service` 패키지**: CLAUDE.md의 도메인 구조는 4개 패키지(controller, dto, entity, repository)였지만, 비즈니스 로직을 둘 곳이 필요해서 `service`를 추가하고 `docs/conventions.md`에 반영했다.

### 모집글·댓글
- **목록의 댓글 수: 글마다 count / `group by` 한 번 / 글 테이블에 댓글 수 컬럼**: 글마다 세면 글 10개에 쿼리가 10번 더 나간다(N+1). 컬럼을 두면 댓글을 쓰고 지울 때마다 맞춰야 한다. 현재 페이지 글 id를 모아 `group by`로 한 번에 세는 방식을 골랐다.
- **작성자 조회**: 목록과 댓글에서 작성자 정보를 매번 따로 가져오지 않도록 fetch join을 썼다. fetch join과 페이징을 함께 쓰려고 `countQuery`를 따로 적었다.
- **글 삭제 시 댓글: DB `ON DELETE CASCADE` / JPA `cascade` / 서비스에서 벌크 delete**: ERD에서 정한 대로 서비스에서 처리했다. JPA cascade는 댓글을 하나씩 지우는 쿼리가 나가서, 벌크 delete 한 번으로 지웠다.
- **페이지 크기 제한**: 명세에 최대값이 없어서 한 번에 너무 많이 가져오지 않도록 1~100으로 제한했다. 벗어나면 400이다.
- **수정에서 `null`의 의미**: PATCH라서 보내지 않은 필드(`null`)는 바꾸지 않는다. 필드를 비우는 기능은 명세에 없어서 따로 구분하지 않았다.
- **마감된 글의 댓글 수정·삭제**: 명세는 작성만 막는다. 이미 쓴 신청을 고치거나 취소하는 것은 허용했다.
- **`isAuthor` 응답 필드 (버그 수정)**: 처음에는 `isAuthor()` getter를 직접 만들었는데, Jackson이 이 getter와 `getAuthor()`를 같은 `author` 속성으로 묶어서 응답에서 `author`가 빠졌다. 그 결과 상세 화면이 하얗게 떴다. 필드에 `@JsonProperty("isAuthor")`를 붙이고 Lombok getter를 만들지 않게(`@Getter(AccessLevel.NONE)`) 바꿔서 해결했다.

### 프론트
- **토큰 저장 위치: `localStorage` / 쿠키**: 백엔드가 헤더 방식 JWT라서 `localStorage`에 저장했다. 시크릿 창 등에서 예외가 날 수 있어서 읽고 쓰는 코드를 `try-catch`로 감쌌다.
- **401 처리 범위**: 모든 401에서 로그인 화면으로 보내면 로그인 실패(401 `USER-002`) 메시지를 보여줄 수 없다. 그래서 **토큰을 보낸 요청**의 401만 로그인 만료로 보고 처리했다.
- **검색 조건 저장 위치: 컴포넌트 상태 / URL 쿼리**: URL 쿼리에 두면 새로고침하거나 뒤로 가도 조건이 유지돼서 URL을 골랐다.
- **삭제 확인: `window.confirm` / 화면 안 확인 버튼**: 브라우저 창은 꾸밀 수 없고 테스트에서 다루기 번거로워서 화면 안에 "삭제 확인"·"취소" 버튼을 두었다.
- **로딩 상태 계산**: `useEffect` 안에서 로딩 상태를 바꾸면 린트 경고(`set-state-in-effect`)가 나서, 응답을 요청 key와 함께 저장하고 key가 다르면 불러오는 중으로 계산했다.
- **enum 대신 문자열 유니온**: tsconfig의 `erasableSyntaxOnly` 설정 때문에 TypeScript `enum`을 쓸 수 없어서 `'PM' | 'DE' | 'BE' | 'FE'` 같은 문자열 유니온을 썼다.

### 테스트
- **E2E 준비 데이터: 화면으로 만들기 / API로 만들기**: 테스트 주제가 아닌 준비 단계(다른 사람 계정, 댓글을 달 글 등)는 API로 만들었다. 빠르고, 각 테스트가 자기 주제만 화면으로 확인할 수 있다. 로그인은 항상 화면으로 한다.
- **E2E 데이터 규칙**: 테스트마다 새 계정을 만들어서 테스트끼리 서로 기대지 않는다. 이메일은 `e2e-`로 시작하고, 비밀번호는 실행할 때마다 `randomUUID()`로 만든다. 같은 DB를 쓰니까 `workers: 1`로 하나씩 실행한다.
- **화면 검사만 할지, 서버도 검사할지**: 버튼을 숨기는 것만으로는 권한을 막았다고 할 수 없어서, E2E에서 API를 직접 호출해 403·409도 확인했다.
- **단위 테스트: `@SpringBootTest` / Mockito**: 서비스 로직만 빠르게 검사하려고 Mockito로 Repository 등을 가짜 객체로 바꿨다. DB 없이 실행된다. 엔티티 id와 요청 DTO에는 setter가 없어서, 프로덕션 코드를 바꾸지 않고 테스트에서 `ReflectionTestUtils`로 값을 넣었다.

## 검증
- **백엔드 빌드**: `backend/`에서 `.\gradlew.bat build`를 실행해 `contextLoads` 테스트를 포함해 `BUILD SUCCESSFUL`을 확인했다. (MySQL 실행 상태)
- **Swagger**: 인증, 모집글, 댓글 순서가 끝날 때마다 Swagger UI에서 13개 API의 정상 응답과 에러 응답(400, 401, 403, 404, 409)을 직접 호출해서 확인했다.
- **`author` 누락 버그**: 수정한 뒤 백엔드를 다시 켜고 상세·댓글 API를 직접 호출해서 응답에 `author`와 `isAuthor`가 둘 다 있는 것을 확인했다.
- **프론트 빌드**: `frontend/`에서 `npm run build`(타입 검사 포함)와 `npm run lint`가 경고 없이 통과했다.
- **화면 수동 확인**: 브라우저에서 회원가입부터 로그인, 마이페이지, 모집글 작성·검색·수정·삭제, 댓글 작성·수정·삭제까지 직접 눌러 보았다.
- **E2E**: 백엔드를 켠 상태에서 `frontend/`에서 `npm run test:e2e`를 실행했다. 결과는 `14 passed (52.5s)`다. (2026-11-19)
- **서비스 단위 테스트**: `backend/`에서 `.\gradlew.bat test --tests "*ServiceTest"`를 실행했고 `BUILD SUCCESSFUL`이 나왔다. 테스트 결과 파일에서 35개 모두 통과(실패·에러·건너뜀 0)를 확인했다. (2026-11-19)

## 남은 이슈
- **버전 차이**: 워크북 기준은 Java 17 / Spring Boot 3.x지만 이 레포는 Java 21 / Spring Boot 4.1.1이다. CLAUDE.md에서 버전 변경을 금지해서 그대로 두었다.
- **전체 테스트 재실행**: 단위 테스트를 추가한 뒤에 `contextLoads`를 포함한 `.\gradlew.bat test` 전체는 다시 실행하지 않았다.
- **Repository 테스트 없음**: 검색 JPQL(`search`), 댓글 수 집계, 벌크 delete는 단위 테스트에서 가짜 객체로 바꿨기 때문에 실제 쿼리는 E2E로만 확인했다. `@DataJpaTest`로 따로 검사할 수 있다.
- **404 화면 없음**: 없는 주소로 들어가면 `/studies`로 보낸다. 주소를 잘못 입력했을 때 알아채기 어렵다.
- **Swagger 에러 스키마**: 모든 컨트롤러에 에러 상태 코드와 설명은 적었다. 하지만 응답 모양(`ErrorResponse` 스키마)은 모집글·댓글 컨트롤러에만 연결했고, `AuthController`와 `UserController`에는 연결하지 않았다. 그래서 Swagger에서 인증·내 정보 API의 에러 응답 형식이 보이지 않는다.
- **블랙리스트 정리**: 만료된 토큰 행이 계속 쌓인다. 데이터가 많아지면 정리하는 작업이 필요하다.
- **토큰 만료 후 동작**: refresh token이 없어서 1시간이 지나면 다시 로그인해야 한다.
- **작업 환경**:
  - 프로젝트가 OneDrive 폴더 안에 있어서 Gradle이 `build` 폴더를 지우지 못하는 문제가 있었다. 동기화를 멈추고 `build`를 지워서 해결했지만, OneDrive 밖으로 옮기는 것이 안전하다.
  - `block-git.ps1` 훅이 한글이 들어간 일부 명령에서 `failed to parse JSON input`으로 git이 아닌 명령까지 막는다. 인코딩 처리를 고쳐야 한다.
