# 코테이토 스터디 매칭 플랫폼 PRD (5, 6주차 과제)

> 이 문서는 Claude Code가 읽고 구현하기 위한 명세예요.
> 모호한 부분은 "쉬운 쪽"으로 구현하고, 가정한 내용은 PR 본문에 적어요.

---

## 0. 배경과 목표

- 현재 코테이토 스터디 신청은 네이버 카페에서 이뤄져요. 이를 대체할 전용 스터디 매칭 플랫폼의 **핵심 기능만** 만들어요.
- 만들 것
    - 이메일/비밀번호 로그인, 로그아웃
    - 회원가입 때 기수·파트 입력, 마이페이지에서 확인
    - 스터디 모집글 작성/조회/검색/수정/삭제
    - 댓글로 스터디 신청 (작성/수정/삭제). **별도 "가입 신청" 기능은 없어요. 댓글 = 신청이에요.**
- 사용자 흐름

```
회원가입(기수, 파트) → 로그인
  → 모집글 작성 / 목록 검색 / 상세 조회
  → 모집글에 댓글로 스터디 신청
  → 마이페이지에서 내 정보 확인
  → 로그아웃
```

- 권한 규칙
    - 조회(목록, 상세, 댓글 목록)는 누구나
    - 작성/수정/삭제는 로그인 사용자만
    - 수정/삭제는 작성자 본인만

---

## 1. 기술 스택

| 구분 | 기술 | 비고 |
| --- | --- | --- |
| 백엔드 | Java 17, Spring Boot 3.x, Spring Data JPA | 빌드는 Gradle |
| 백엔드 | Spring Security, JWT | 비밀번호는 BCrypt 암호화 |
| 백엔드 | 1-3주차에서 쓴 DB, springdoc(Swagger), JUnit 5 | DB는 1-3주차와 동일하게 |
| 프론트 | React, TypeScript, Vite, Tailwind CSS | 필수 |
| 프론트 | React Router, axios 또는 fetch | |
| 프론트 | TanStack Query 등 | 선택 |
| 도구 | Git/GitHub, ERDCloud, Claude Code | |
| 도구 | Playwright | E2E 테스트 필수 |

- **Next.js 금지.** Vite 기반 React SPA.
- 위 목록 외 라이브러리를 추가하면 PR에 이유를 적어요.

---

## 2. 모노레포 구조

```
14th-Backend-Assignment/
├── CLAUDE.md                  # 4주차 것을 도메인에 맞게 갱신
├── .claude/                   # 4주차 하네스 (필요하면 추가)
├── .dev/
├── .github/                   # 이슈, PR 템플릿
├── docs/
│   ├── erd/                   # ERD png + ERDCloud 링크
│   ├── api-spec.md            # API 명세
│   └── features/              # PR 정리 문서
├── backend/                   # 기존 루트의 src/, build.gradle 등을 여기로 이동
│   ├── src/
│   ├── build.gradle
│   └── .env.example           # 환경변수 이름만 (값은 비움)
└── frontend/
    ├── src/
    ├── e2e/                   # Playwright E2E
    ├── package.json
    ├── playwright.config.ts
    └── vite.config.ts
```

### backend/ 이동 시 같이 고칠 것
- `CLAUDE.md`의 빌드/테스트 명령어와 경로
- `.claude/hooks/` 스크립트와 `.claude/settings.json`의 경로
- `.claude/rules/` 안의 파일 경로 예시
- `.env`는 `backend/` 안으로 이동 (서버를 `backend/`에서 실행해서 읽음). `.env.example`도 `backend/` 안.
- `.env`, `node_modules/`, `playwright-report/`, `test-results/`가 Git에 올라가지 않는지 `.gitignore`로 확인

---

## 3. 브랜치와 제출 규칙

```bash
git checkout 이름/basic
git pull origin 이름/basic       # 4주차 하네스가 들어 있어야 함
git checkout -b 이름/monorepo
```

- 작업 브랜치는 `이름/monorepo` **하나만**. 기능별 브랜치 없음.
- **PR은 1개.** `이름/monorepo → 이름/basic` (**base는 develop이 아님!**)
- 중간에는 `git push origin 이름/monorepo`로 백업만.
- 커밋은 논리 단위로, **프로덕션 코드와 테스트 코드 커밋 분리.**
- 문서는 `docs/features/이슈번호-기능명.md` **1개**. 안에서 인증 → 모집글 → 댓글 순으로 정리 (4주차 1-2장 형식).

### 작업 순서 (순서마다 커밋)

| 순서 | 작업 | 포함 내용 |
| --- | --- | --- |
| 0 | 프로젝트 세팅 | 모노레포 전환, ERD, CLAUDE.md 갱신, docs/api-spec.md |
| 1 | 인증 | 회원가입(기수, 파트), 로그인, 로그아웃, 내 정보, 마이페이지 (백+프론트) |
| 2 | 모집글 | 작성, 목록(검색/필터), 상세, 수정, 삭제 (백+프론트) |
| 3 | 댓글(신청) | 작성, 목록, 수정, 삭제 (백+프론트) |
| 4 | E2E 테스트 | Playwright로 인증/모집글/댓글 흐름 검증 (정상 + 에러). 테스트 코드는 별도 커밋 |

---

## 4. 데이터 모델 / ERD

아래는 과제 원문의 **최소 구성**이에요. 컬럼 이름과 의미는 바꾸지 않아요.

### User
| 컬럼 | 비고 |
| --- | --- |
| id | PK |
| email | **UK** (unique) |
| password | BCrypt 해시 |
| name | |
| nickname | 2~10자 |
| generation | 1 이상의 정수 |
| part | PM, DE, BE, FE 중 하나 (Enum 문자열) |
| created_at | |
| updated_at | |

### StudyPost
| 컬럼 | 비고 |
| --- | --- |
| id | PK |
| user_id | FK → User (작성자) |
| title | 1~50자 |
| content | 1~2000자 |
| study_status | RECRUITING 또는 CLOSED (Enum 문자열). 생성 시 RECRUITING |
| created_at | |
| updated_at | |

### StudyPostComment
| 컬럼 | 비고 |
| --- | --- |
| id | PK |
| study_post_id | FK → StudyPost |
| user_id | FK → User (작성자) |
| content | 1~500자. **댓글이 곧 스터디 신청이에요** |
| created_at | |
| updated_at | |

### part 의미
| part | 의미 |
| --- | --- |
| PM | 기획 |
| DE | 디자인 |
| BE | 백엔드 |
| FE | 프론트엔드 |

- 기수(generation)와 파트(part)는 회원가입 때 본인이 입력하는 값이고, 별도로 검증하지 않아요. (형식 검증만: 기수 1 이상 정수, 파트 4개 중 하나)
- 클래스/테이블 이름은 `User`, `StudyPost`, `StudyPostComment`로 맞춰요. (API 경로의 `comments`와는 별개)
- `User`는 DB 예약어 충돌 가능성이 있으니 테이블명은 `users` 등으로 매핑해도 돼요. 이 경우 ERD와 PR에 적어요.

### (추가 테이블 예시) 토큰 블랙리스트 — 로그아웃용
| 컬럼 | 비고 |
| --- | --- |
| token(또는 jti) | 로그아웃한 토큰 |
| expires_at | 토큰 만료 시각. 만료 후 정리 가능 |

저장 방식은 자유 (DB, 인메모리).

### 규칙
- 연관관계는 **모두 LAZY**, Enum은 **문자열(`EnumType.STRING`)** 로 저장
- 모집글 삭제 시 댓글도 함께 삭제 (cascade 또는 명시적 삭제)
- 컬럼/테이블을 추가해도 되지만: 기존 컬럼의 이름·의미는 바꾸지 않고, 추가분은 **ERD, docs/api-spec.md, PR의 ERD/문서 변경 칸**에 함께 적어요.
- ERD는 **ERDCloud**에서 직접 그리고, `docs/erd/`에 png와 링크를 올려요. 테이블이 바뀌면 ERD도 같이 고쳐요.

---

## 5. 공통 규칙 (백엔드)

- 모든 API 주소는 `/api`로 시작
- 인증 필요 API는 `Authorization: Bearer 토큰` 헤더
- 권한: **공개**(인증 불필요) / **로그인**(토큰 필요). 수정·삭제는 추가로 **작성자 본인**
- 성공 응답: 3주차의 **공통 응답으로 감싸요.** 아래 명세의 응답은 `data` 안쪽만 적은 거예요. 본문 없는 `204`는 감싸지 않고 그대로.
- 실패 응답: 모든 API에서 3주차 `ErrorResponse` 형식

```json
{
  "success": false,
  "status": 404,
  "code": "STUDY-001",
  "message": "존재하지 않는 스터디 모집글이에요.",
  "method": "GET",
  "path": "/api/study-posts/999",
  "timestamp": "2026-11-08 12:00:00"
}
```

- `code`는 `도메인 prefix-일련번호`(예: `STUDY-001`). 문서의 `STUDY_POST_NOT_FOUND` 등은 **`ErrorCode` enum 상수 이름**이에요. (3주차의 `BOOK_NOT_FOUND` ↔ `BOOK-001`과 같은 관계)
- `ErrorCode`는 3주차처럼 **enum 하나에 모으고**, 새 도메인은 새 그룹으로 추가
- 페이지 응답: `content`, `page`, `size`, `totalElements`, `totalPages`
- 작성자 정보는 `author` 객체: `nickname`, `generation`, `part`
- 명세에 없는 세부 규칙은 **쉬운 쪽**으로 (모집 상태는 RECRUITING↔CLOSED 자유 전환 / 같은 사람이 댓글 여러 번 가능 / 토큰은 `accessToken` 하나)
- API 명세에 없는 필드를 더하는 건 괜찮지만, **명세에 있는 필드는 그대로** 지켜요.
- 인증 실패(토큰 없음/무효/만료/로그아웃된 토큰)는 모두 `401 UNAUTHORIZED`.
- 인증/인가 예외(401, 403)도 `ErrorResponse` 형식으로 나가도록 Security의 `AuthenticationEntryPoint`, `AccessDeniedHandler`를 설정해요.
- CORS: 프론트 `http://localhost:3000` 허용 (3주차 설정 확인).

---

## 6. API 명세 (13개)

### 인증

**1. 회원가입** `POST /api/auth/signup` — 공개
```
[Request]
{ "email": "user@gmail.com", "password": "password1234", "name": "홍길동",
  "nickname": "길동이", "generation": 14, "part": "BE" }
[Response 201] { "userId": 1 }
[검증] email 형식 / password 8자 이상 / nickname 2~10자
       generation 1 이상 정수 / part는 PM, DE, BE, FE 중 하나
[에러] 400 INVALID_INPUT, 409 EMAIL_ALREADY_EXISTS
```

**2. 로그인** `POST /api/auth/login` — 공개
```
[Request]  { "email": "user@gmail.com", "password": "password1234" }
[Response 200] { "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
[에러] 401 INVALID_CREDENTIALS
```

**3. 로그아웃** `POST /api/auth/logout` — 로그인
```
[Response 204] 본문 없음
[규칙] - 서버는 사용한 토큰을 만료 시각까지 무효 처리 (저장 방식 자유)
       - 로그아웃한 토큰으로 다시 요청하면 401
       - 프론트는 저장한 토큰을 지우고 로그인 화면으로 이동
[에러] 401 UNAUTHORIZED
```

**4. 내 정보 조회** `GET /api/users/me` — 로그인
```
[Response 200]
{ "userId": 1, "email": "user@gmail.com", "name": "홍길동", "nickname": "길동이",
  "generation": 14, "part": "BE" }
[규칙] 비밀번호는 절대 응답에 넣지 않아요.
[에러] 401 UNAUTHORIZED
```

### 스터디 모집글

**5. 모집글 작성** `POST /api/study-posts` — 로그인
```
[Request] { "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }
[Response 201] { "postId": 10 }
[검증] title 1~50자 / content 1~2000자
[규칙] 처음 만들면 study_status는 RECRUITING
[에러] 400 INVALID_INPUT, 401 UNAUTHORIZED
```

**6. 모집글 목록** `GET /api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10` — 공개
```
[Query]
keyword (선택) 제목, 내용에 포함된 키워드
status  (선택) RECRUITING | CLOSED. 없으면 전체
part    (선택) PM | DE | BE | FE. 작성자의 파트가 같은 글만. 없으면 전체
page    (선택) 기본 0
size    (선택) 기본 10

[Response 200] 최신순
{
  "content": [
    { "postId": 10, "title": "알고리즘 스터디 모집", "studyStatus": "RECRUITING",
      "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
      "commentCount": 3, "createdAt": "2026-11-08T12:00:00" }
  ],
  "page": 0, "size": 10, "totalElements": 1, "totalPages": 1
}
[규칙] part는 글 작성자의 파트로 거르므로 모집글과 User를 조인해야 해요.
       keyword, status, part는 함께 쓸 수 있고, 함께 쓰면 모두 만족하는 글만.
[에러] 400 INVALID_INPUT (status나 part 값이 올바르지 않을 때)
```
> 구현 팁: 동적 조건은 QueryDSL/Specification/JPQL 중 편한 것으로. `commentCount`와 `author` 접근에서 **N+1이 생기지 않게** fetch join 또는 배치 조회를 쓰고, 테스트로 확인해요.

**7. 모집글 상세** `GET /api/study-posts/{postId}` — 공개
```
[Response 200]
{ "postId": 10, "title": "...", "content": "...", "studyStatus": "RECRUITING",
  "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
  "isAuthor": true,
  "createdAt": "2026-11-08T12:00:00", "updatedAt": "2026-11-08T12:00:00" }
[규칙] 로그인하지 않았으면 isAuthor는 false
       (공개 API여도 토큰이 있으면 파싱해서 isAuthor를 계산해야 해요)
[에러] 404 STUDY_POST_NOT_FOUND
```

**8. 모집글 수정** `PATCH /api/study-posts/{postId}` — 작성자
```
[Request] 바꿀 필드만
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }
[Response 200] { "postId": 10 }
[에러] 400 INVALID_INPUT, 403 NOT_POST_AUTHOR, 404 STUDY_POST_NOT_FOUND
```

**9. 모집글 삭제** `DELETE /api/study-posts/{postId}` — 작성자
```
[Response 204] 본문 없음
[규칙] 글을 삭제하면 달린 댓글도 함께 삭제
[에러] 403 NOT_POST_AUTHOR, 404 STUDY_POST_NOT_FOUND
```

### 댓글 = 스터디 신청

**10. 댓글 작성** `POST /api/study-posts/{postId}/comments` — 로그인
```
[Request] { "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요." }
[Response 201] { "commentId": 5 }
[검증] content 1~500자
[규칙] 모집이 마감된(CLOSED) 글에는 댓글을 쓸 수 없어요.
[에러] 400 INVALID_INPUT, 401 UNAUTHORIZED, 404 STUDY_POST_NOT_FOUND, 409 STUDY_POST_CLOSED
```

**11. 댓글 목록** `GET /api/study-posts/{postId}/comments?page=0&size=20` — 공개
```
[Response 200] 오래된 순
{ "content": [
    { "commentId": 5, "content": "안녕하세요! ...",
      "author": { "nickname": "철수", "generation": 13, "part": "FE" },
      "isAuthor": false, "createdAt": "2026-11-08T13:00:00" } ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1 }
[에러] 404 STUDY_POST_NOT_FOUND
```

**12. 댓글 수정** `PATCH /api/comments/{commentId}` — 작성자
```
[Request] { "content": "수정한 신청 메시지" }
[Response 200] { "commentId": 5 }
[에러] 400 INVALID_INPUT, 403 NOT_COMMENT_AUTHOR, 404 COMMENT_NOT_FOUND
```

**13. 댓글 삭제** `DELETE /api/comments/{commentId}` — 작성자
```
[Response 204] 본문 없음
[에러] 403 NOT_COMMENT_AUTHOR, 404 COMMENT_NOT_FOUND
```

### 에러 코드 요약

| HTTP | ErrorCode 상수 | 응답 code(예시) | 설명 |
| --- | --- | --- | --- |
| 400 | INVALID_INPUT | COMMON-003 | 요청 값 검증 실패 (기수가 1 미만이거나 파트가 목록에 없는 경우 포함) |
| 401 | INVALID_CREDENTIALS | USER-002 | 이메일 또는 비밀번호가 틀려요 |
| 401 | UNAUTHORIZED | COMMON-004 | 토큰이 없거나, 유효하지 않거나, 로그아웃된 토큰이에요 |
| 403 | NOT_POST_AUTHOR / NOT_COMMENT_AUTHOR | STUDY-002 / COMMENT-002 | 작성자가 아니에요 |
| 404 | STUDY_POST_NOT_FOUND / COMMENT_NOT_FOUND | STUDY-001 / COMMENT-001 | 대상이 없어요 |
| 409 | EMAIL_ALREADY_EXISTS | USER-001 | 이미 가입된 이메일이에요 |
| 409 | STUDY_POST_CLOSED | STUDY-003 | 모집이 마감된 글에는 댓글을 쓸 수 없어요 |

---

## 7. 프론트엔드

### 화면

| 화면 | 경로 | 필요한 동작 |
| --- | --- | --- |
| 회원가입 | `/signup` | 이메일, 비밀번호, 이름, 닉네임, 기수, 파트(선택 목록) 입력. 검증 메시지. 성공 시 로그인 화면으로 이동 |
| 로그인 | `/login` | 성공 시 토큰 저장 후 목록으로 이동, 실패 시 에러 메시지 |
| 스터디 목록 | `/studies` | 키워드 검색, 모집 상태 필터, 파트 필터, 페이지 이동. 각 글에 작성자 닉네임/기수/파트 표시 |
| 스터디 상세 | `/studies/:id` | 내용, 댓글 목록, 댓글 작성/수정/삭제. 작성자에게만 글 수정/삭제/모집 마감 버튼 표시 |
| 스터디 작성/수정 | `/studies/new`, `/studies/:id/edit` | 제목, 내용 입력. 수정 화면에서는 모집 상태도 변경 가능 |
| 마이페이지 | `/mypage` | 이메일, 이름, 닉네임, 기수, 파트 표시 |

### 공통으로 지킬 것
- 헤더는 로그인 상태에 따라 달라져요: 비로그인 = 로그인/회원가입 버튼, 로그인 = 닉네임/마이페이지/로그아웃
- 로그인이 필요한 화면에 로그인 없이 들어가면 `/login`으로 보내요 (보호 라우트)
- 서버가 내려준 에러 메시지를 화면에 보여주고, **로딩 상태도 표시**해요
- 입력창에는 **`label`**, 버튼은 **`button`**, 이동은 **링크(`a`)** 로 만들어요 (E2E가 role/label로 요소를 찾음)
- 토큰이 만료/무효가 되어 **401이 오면 로그인 화면으로** 보내요 (axios interceptor 등)
- 디자인은 자유. 대신 **Tailwind CSS**, 모바일 화면에서도 깨지지 않게
- 개발 서버 포트는 **3000**. `vite.config.ts`에 `server: { port: 3000, strictPort: true }`
- API 호출은 `src/api/` 로 모으고, 타입은 명세의 필드명을 그대로 써요.

---

## 8. E2E 테스트 (Playwright, 필수)

- 화면 녹화 대신 E2E 테스트로 프론트-백엔드 연동을 검증해요. **전부 통과한 결과를 터미널 캡처로 PR에 첨부.**
- 준비 (`frontend/`에서)

```bash
npm install -D @playwright/test
npx playwright install chromium
```

| 명령어 | 하는 일 |
| --- | --- |
| `npx playwright test` | 전체 실행 (화면 없이) |
| `npx playwright test e2e/auth.spec.ts` | 파일 하나만 |
| `npx playwright test -g "로그인"` | 제목에 "로그인" 들어간 테스트만 |
| `npx playwright test --headed` | 크롬 창이 뜨며 실행 |
| `npx playwright test --ui` | UI 모드 |
| `npx playwright show-report` | 리포트 열기 |

### 실행 순서
1. 백엔드 서버와 DB를 먼저 켜요 (`backend/`에서)
2. `frontend/`에서 `npx playwright test`. 프론트 서버는 `playwright.config.ts`의 `webServer`에 적어 두면 자동 실행

### 꼭 지킬 것
- 요소는 `getByRole`, `getByLabel`, `getByText`로 찾아요. **CSS 선택자에 기대지 않아요.**
- `waitForTimeout` 같은 **고정 대기 금지.** `expect(...)`의 자동 대기를 써요.
- **테스트마다 독립.** 이메일/닉네임/글 제목은 매번 새로 만들어요 (UUID, 타임스탬프). 앞 테스트 데이터에 기대지 않아요.
- 테스트 계정의 **비밀번호를 코드에 적지 않아요.** 실행할 때 생성하거나 환경변수로 넣어요.
- 개발 DB에 쌓이는 테스트 데이터는 이메일을 **`e2e-`로 시작**하게 해서 구분해요.
- 실패하면 **원인이 앱인지 테스트인지 먼저 가려요.** 통과시키려고 테스트를 지우거나 검사를 약하게 바꾸지 않아요.
- 화면 글자(버튼 이름, 라벨)는 내 화면에 맞게 만들어요.
- 화면에서 버튼을 숨긴 동작(남의 글 수정 등)은 `request` fixture로 **API를 직접 호출**해서 403, 409가 오는지도 확인해요.

### 반드시 들어갈 시나리오 (정상 + 에러)

**인증 (`e2e/auth.spec.ts`)**
- 회원가입(기수, 파트 선택) → 로그인 화면 이동 → 로그인 성공 → 목록으로 이동
- 마이페이지에 이메일/이름/닉네임/기수/파트 표시
- 로그아웃 후 로그인 화면으로 이동, 헤더가 비로그인 상태로 복귀
- 로그아웃한 토큰으로 API 호출 시 401
- 중복 이메일 가입 → 에러 메시지 (409)
- 잘못된 비밀번호 로그인 → 에러 메시지 (401)
- 입력 검증 실패 (비밀번호 8자 미만 등) → 검증 메시지
- 비로그인으로 `/studies/new`, `/mypage` 접근 → 로그인 화면으로 이동

**모집글 (`e2e/study-post.spec.ts`)**
- 작성 → 상세에서 내용 확인 (작성자 닉네임/기수/파트 포함)
- 목록에서 키워드 검색, 모집 상태 필터, 파트 필터 (조합 포함), 페이지 이동
- 작성자가 수정 (제목/내용/모집 상태 변경), 삭제 후 목록에서 사라짐
- 다른 사용자에게는 수정/삭제 버튼이 보이지 않음 + API 직접 호출 시 403
- 없는 글 상세 접근 → 404 처리 화면/메시지

**댓글 (`e2e/comment.spec.ts`)**
- 댓글 작성 → 목록에 표시, 작성자만 수정/삭제 가능
- 모집 마감(CLOSED) 글에는 댓글 작성 불가 (화면 + API 409)
- 남의 댓글 수정/삭제 API 직접 호출 시 403
- 글 삭제 시 댓글도 함께 삭제됨

> 시나리오와 결과를 PR에 붙이는 방법은 원 과제 6-4장 형식을 따라요.

---

## 9. 하네스 이어 쓰기

4주차에 만든 하네스(`CLAUDE.md`, rules, skills, hooks)를 **그대로 가지고** 개발하고, 필요하면 추가해요.

| 할 것 | 설명 |
| --- | --- |
| `CLAUDE.md` 갱신 | 도메인 설명, 모노레포 구조, 백엔드/프론트 실행 명령어를 더해 프로젝트 개요를 고쳐요 |
| `docs/` 추가 | 도메인 문서, ERD, API 명세 |
| 규칙 추가 | 개발하면서 AI가 반복해서 틀리는 것은 rules로 (예: 프론트 컨벤션, API 응답 형식) |
| 스킬/훅/서브에이전트 추가 | 반복 작업은 스킬로, 막고 싶은 행동은 훅으로 |
| E2E 규칙/스킬 (선택) | 고정 대기, 테스트끼리 데이터 공유, 비밀번호 코드 하드코딩 → rules / 실행 절차 → 스킬 |
| `docs/features/` 작성 | PR에 맞는 기능 문서 1개 (인증, 모집글, 댓글 순) |

- 하네스를 새로 추가하거나 고쳤다면 **PR 본문의 "하네스 활용" 칸**에 무엇을, 왜 바꿨는지 적어요.

---

## 10. 제출 전 최종 점검 (원문 13개 항목)

제출 전에 Claude Code에게 아래 13개를 **하나씩 실제로 확인**시키고, 근거(명령어 결과, 파일 경로)와 함께 통과/미통과로 보고받아요.

| # | 항목 | 확인 기준 | 확인 방법 | 통과 |
| --- | --- | --- | --- | --- |
| 1 | 구조 | `backend/`와 `frontend/`가 한 레포(모노레포)에 있어요 | 루트 `ls`, 루트에 옛 `src/`, `build.gradle`이 남아 있지 않은지 | [ ] |
| 2 | 스택 | 프론트는 React, TypeScript, Vite, Tailwind CSS이고 Next.js를 쓰지 않았어요 | `frontend/package.json` 의존성 확인 (`next` 없음) | [ ] |
| 3 | ERD | ERDCloud로 그렸고, `docs/erd/`에 이미지와 링크가 있어요. **실제 테이블과 같아요** | ERD png와 엔티티 클래스/DB 스키마 대조 | [ ] |
| 4 | API 구현 | 13개 API가 명세대로 동작하고 Swagger에서 호출돼요. 목록 API는 키워드, 모집 상태, 파트 필터가 모두 동작해요 | Swagger에서 13개 호출, 필터 조합 호출 | [ ] |
| 5 | 입력 검증 | 기수는 1 이상의 정수, 파트는 PM, DE, BE, FE 중 하나만 받아요 | 기수 0, 파트 `XX`로 가입 시 400 `INVALID_INPUT` | [ ] |
| 6 | 권한 | 공개, 로그인, 작성자 권한이 명세대로 막혀요 | 토큰 없이 작성 401, 남의 글/댓글 수정·삭제 403 | [ ] |
| 7 | 프론트 화면 | 6개 화면이 동작하고 공통 규칙(헤더, 로그인 이동, 에러 표시)을 지켜요 | 7장 화면 표와 공통 규칙 대조, 모바일 폭 확인 | [ ] |
| 8 | 하네스 | `CLAUDE.md`가 도메인에 맞게 갱신됐고, 경로가 모노레포에 맞아요 | `CLAUDE.md`, `.claude/hooks`, `settings.json`, `rules`의 경로 확인 | [ ] |
| 9 | 기능 문서 | PR 문서 `docs/features/이슈번호-기능명.md` 1개가 있고, 인증, 모집글, 댓글이 정리돼 있어요 | 파일 1개, 3개 섹션 순서 확인 | [ ] |
| 10 | E2E 테스트 | `frontend/e2e/`에 인증, 모집글, 댓글 흐름의 정상, 에러 시나리오가 있고 `npx playwright test`가 모두 통과해요. 통과 결과 캡처가 PR에 있어요 | 백엔드 기동 후 `npx playwright test` 실행, 터미널 캡처 | [ ] |
| 11 | PR 규칙 | PR은 1개이고, base가 `이름/basic`이며, `closes #이슈번호`로 이슈와 연결했어요 | PR 화면에서 base 브랜치와 본문 확인 | [ ] |
| 12 | 민감 정보 | `.env`, DB 계정, JWT 시크릿이 Git에 올라가지 않았어요 | `git ls-files`, `git log -p`에서 시크릿 검색, `.gitignore` 확인 | [ ] |
| 13 | 테스트 | 백엔드는 핵심 로직(서비스)의 단위 테스트가 있고 `./gradlew test`가 통과해요. 프론트는 `npm run build`가 통과해요 | `backend/`에서 `./gradlew test`, `frontend/`에서 `npm run build` | [ ] |

### 추가로 같이 확인할 것 (원문 항목을 보완하는 자체 점검)
- [ ] `이름/basic`에서 `이름/monorepo`를 땄고, 커밋이 작업 순서(0~4)대로 나뉘며 프로덕션/테스트 커밋이 분리돼 있다
- [ ] `.env.example`이 `backend/` 안에 있고 값은 비어 있다. `node_modules/`, `playwright-report/`, `test-results/`도 git에 없다
- [ ] 성공은 공통 응답, 실패는 `ErrorResponse`(401/403 포함), 204는 본문 없음. `ErrorCode`는 enum 하나에 `도메인-번호` 형식
- [ ] 비밀번호 BCrypt, 응답에 비밀번호 없음, 로그아웃한 토큰은 401
- [ ] 연관관계 LAZY, Enum STRING, 목록 조회에서 N+1 없음, 글 삭제 시 댓글 삭제
- [ ] 프론트 개발 서버 포트 3000 고정, label/button/a 사용, 401이면 로그인 화면으로 이동
- [ ] PR 본문에 추가한 라이브러리 이유, ERD/문서 변경, 하네스 활용(무엇을, 왜 바꿨는지)이 적혀 있다

### 점검 요청 프롬프트 (Claude Code용)

```
docs/PRD.md의 10장 "제출 전 최종 점검" 13개 항목을 하나씩 실제로 확인해줘.
- 각 항목마다 실행한 명령어와 결과를 근거로 들어서 통과/미통과로 표시해줘. 추측으로 통과 처리하지 마.
- 미통과 항목은 원인과 고칠 방법을 적고, 내가 승인하면 고쳐줘.
- 10번(E2E)과 13번(테스트)은 백엔드 서버와 DB를 켠 상태에서 실제로 실행해줘. 실패하면 테스트를 지우거나 약하게 바꾸지 말고 원인이 앱인지 테스트인지 먼저 가려줘.
- 11번(PR)은 내가 PR을 만든 뒤 확인할 수 있게, 필요한 PR 본문 초안(closes #이슈번호, 하네스 활용 칸 포함)을 따로 만들어줘.
- 마지막에 13개 결과를 표로 요약해줘.
```