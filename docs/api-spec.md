# API 명세 — 코테이토 스터디 매칭 플랫폼

5,6주차 과제 요구사항을 정리한 문서예요. 구현은 이 문서를 기준으로 해요.

- ERD: [docs/erd/](./erd/)
- 명세에 없는 필드를 요청이나 응답에 더하는 건 괜찮지만, 명세에 있는 필드는 그대로 지켜요.
- 명세에 없는 세부 규칙은 쉬운 쪽으로 구현해요.
  - 모집 상태는 RECRUITING과 CLOSED 사이를 자유롭게 바꿀 수 있어요.
  - 같은 사람이 댓글을 여러 번 달아도 돼요.
  - 토큰은 accessToken 하나만 써요.

## 목차

1. [사용자 흐름과 권한](#1-사용자-흐름과-권한)
2. [데이터 모델](#2-데이터-모델)
3. [공통 규칙](#3-공통-규칙)
4. [API 목록](#4-api-목록)
5. [인증 API (1~4번)](#5-인증-api-14번)
6. [스터디 모집글 API (5~9번)](#6-스터디-모집글-api-59번)
7. [댓글 API (10~13번)](#7-댓글-api-1013번)
8. [에러 코드](#8-에러-코드)
9. [프론트엔드 화면](#9-프론트엔드-화면)
10. [구현 메모](#10-구현-메모)

---

## 1. 사용자 흐름과 권한

```
회원가입(기수, 파트 입력) → 로그인
        → 스터디 모집글 작성 / 목록 검색 / 상세 조회
        → 모집글에 댓글로 스터디 신청
        → 마이페이지에서 내 정보(기수, 파트 포함) 확인
        → 로그아웃
```

- 별도의 "가입 신청" 기능은 없어요. 모집글에 댓글을 남기는 것이 곧 스터디 신청이에요.
- 조회는 누구나 할 수 있어요. (목록, 상세, 댓글 목록)
- 작성, 수정, 삭제는 로그인한 사용자만 할 수 있어요.
- 수정과 삭제는 작성자 본인만 할 수 있어요.

## 2. 데이터 모델

| 테이블 | 컬럼 | 비고 |
|---|---|---|
| User | id, email(UK), password, name, nickname, generation, part, created_at, updated_at | generation은 1 이상의 정수, part는 PM, DE, BE, FE 중 하나 |
| StudyPost | id, user_id(FK), title, content, study_status, created_at, updated_at | study_status는 RECRUITING 또는 CLOSED |
| StudyPostComment | id, study_post_id(FK), user_id(FK), content, created_at, updated_at | 댓글이 곧 스터디 신청 |

| part | 의미 |
|---|---|
| PM | 기획 |
| DE | 디자인 |
| BE | 백엔드 |
| FE | 프론트엔드 |

- 기수와 파트는 회원가입 때 본인이 입력하는 값이에요. 별도로 검증하지 않아요.
- 위는 최소 구성이에요. 필요하면 컬럼이나 테이블을 추가해도 돼요. (예: 로그아웃한 토큰을 저장할 테이블)
- 위에 적힌 컬럼의 이름과 의미는 바꾸지 않아요.
- 추가한 컬럼과 테이블은 ERD, 이 문서, PR의 "ERD/문서 변경"에 함께 적어요.
- 연관관계는 모두 LAZY로 하고, Enum은 문자열로 저장해요.
- 비밀번호는 BCrypt로 암호화해서 저장해요.

## 3. 공통 규칙

- 모든 API의 주소는 `/api`로 시작해요. 3주차 예제(`/hello`, `/books`)는 이 규칙의 예외로 그대로 둬요.
- 인증이 필요한 API는 요청 헤더에 `Authorization: Bearer 토큰`을 담아 보내요.
- 권한은 두 가지로 나눠요: **공개**(인증 불필요), **로그인**(토큰 필요). 수정과 삭제는 여기에 더해 **작성자** 본인이어야 해요.
- 응답은 3주차에서 만든 공통 응답 `ApiResponse<T>`(success, status, data, timestamp)로 감싸요. 200은 `ResponseUtils.ok`, 201은 `ResponseUtils.created`로 만들어요. 아래 명세의 응답 예시는 `data` 안쪽만 적은 거예요.
- 본문이 없는 204 응답은 감싸지 않고 그대로 내려줘요. (`ResponseEntity.noContent().build()`)
- 날짜 형식: 공통 응답의 `timestamp`는 `yyyy-MM-dd HH:mm:ss`, `data` 안의 날짜(`createdAt`, `updatedAt`)는 ISO 형식(`2026-11-08T12:00:00`)이에요.
- 목록 API의 페이지 응답은 `content`, `page`, `size`, `totalElements`, `totalPages`를 내려줘요.
- 작성자 정보는 `author` 객체로 내려줘요: `nickname`, `generation`, `part`

### 실패 응답

모든 API에서 3주차의 `ErrorResponse`와 같은 형식으로 내려줘요. (success, status, code, message, method, path, timestamp)

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

- 응답의 `code`는 3주차와 같이 도메인 prefix + 하이픈 + 일련번호예요. (예: `STUDY-001`)
- 이 문서의 `STUDY_POST_NOT_FOUND` 같은 이름은 `ErrorCode` enum의 상수 이름이에요. (3주차의 `BOOK_NOT_FOUND`가 `BOOK-001`인 것과 같은 관계)
- `ErrorCode`는 3주차처럼 enum 하나에 모아서 관리하고, 새 도메인은 새 그룹을 추가해요.
- 400 요청 값 오류는 `INVALID_INPUT`만 써요. 기존 `COMMON_BAD_REQUEST`(`COMMON-001`)는 쓰지 않아요. (구현할 때 지울 후보)
- 검증 실패(`@Valid`), 잘못된 쿼리 값, Security 필터에서 나는 401도 이 형식으로 내려줘야 해요. 3주차 `GlobalExceptionHandler`는 `Backend14thException`과 그 밖의 `Exception`(500)만 처리하므로, 구현할 때 이 경우들의 처리를 추가해야 해요.

## 4. API 목록

| # | Method | URL | 권한 | 설명 |
|---|---|---|---|---|
| 1 | POST | /api/auth/signup | 공개 | 회원가입 (기수, 파트 포함) |
| 2 | POST | /api/auth/login | 공개 | 로그인 (토큰 발급) |
| 3 | POST | /api/auth/logout | 로그인 | 로그아웃 (토큰 무효화) |
| 4 | GET | /api/users/me | 로그인 | 내 정보 조회 (기수, 파트 포함) |
| 5 | POST | /api/study-posts | 로그인 | 모집글 작성 |
| 6 | GET | /api/study-posts | 공개 | 모집글 목록 (검색, 모집 상태와 파트 필터, 페이징) |
| 7 | GET | /api/study-posts/{postId} | 공개 | 모집글 상세 |
| 8 | PATCH | /api/study-posts/{postId} | 작성자 | 모집글 수정 (모집 상태 변경 포함) |
| 9 | DELETE | /api/study-posts/{postId} | 작성자 | 모집글 삭제 (댓글도 함께 삭제) |
| 10 | POST | /api/study-posts/{postId}/comments | 로그인 | 댓글(신청) 작성 |
| 11 | GET | /api/study-posts/{postId}/comments | 공개 | 댓글 목록 |
| 12 | PATCH | /api/comments/{commentId} | 작성자 | 댓글 수정 |
| 13 | DELETE | /api/comments/{commentId} | 작성자 | 댓글 삭제 |

---

## 5. 인증 API (1~4번)

### 1. 회원가입 `POST /api/auth/signup`

권한: 공개

Request

```json
{
  "email": "user@gmail.com",
  "password": "password1234",
  "name": "홍길동",
  "nickname": "길동이",
  "generation": 14,
  "part": "BE"
}
```

Response `201`

```json
{ "userId": 1 }
```

검증

- email: 이메일 형식
- password: 8자 이상
- nickname: 2~10자
- generation: 1 이상의 정수
- part: PM, DE, BE, FE 중 하나

에러

- 400 `INVALID_INPUT`
- 409 `EMAIL_ALREADY_EXISTS`

### 2. 로그인 `POST /api/auth/login`

권한: 공개

Request

```json
{ "email": "user@gmail.com", "password": "password1234" }
```

Response `200`

```json
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
```

에러

- 401 `INVALID_CREDENTIALS`

### 3. 로그아웃 `POST /api/auth/logout`

권한: 로그인

Response `204` 본문 없음

규칙

- 서버는 사용한 토큰을 만료 시각까지 무효 처리해요. (저장 방식은 자유: DB, 인메모리 등)
  - 이 프로젝트는 **인메모리**로 저장해요. (토큰 → 만료 시각) 그래서 추가 테이블이 없어요.
  - 한계: 서버를 재시작하면 목록이 사라져서, 로그아웃한 토큰이 만료 전까지 다시 유효해져요.
- 로그아웃한 토큰으로 다시 요청하면 401을 내려줘요.
- 프론트는 저장해둔 토큰을 지우고 로그인 화면으로 이동해요.

에러

- 401 `UNAUTHORIZED`

### 4. 내 정보 조회 `GET /api/users/me`

권한: 로그인

Response `200`

```json
{
  "userId": 1,
  "email": "user@gmail.com",
  "name": "홍길동",
  "nickname": "길동이",
  "generation": 14,
  "part": "BE"
}
```

규칙

- 비밀번호는 절대 응답에 넣지 않아요.

에러

- 401 `UNAUTHORIZED`

---

## 6. 스터디 모집글 API (5~9번)

### 5. 모집글 작성 `POST /api/study-posts`

권한: 로그인

Request

```json
{ "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }
```

Response `201`

```json
{ "postId": 10 }
```

검증

- title: 1~50자
- content: 1~2000자

규칙

- 처음 만들면 study_status는 RECRUITING이에요.

에러

- 400 `INVALID_INPUT`
- 401 `UNAUTHORIZED`

### 6. 모집글 목록 `GET /api/study-posts`

권한: 공개

예: `GET /api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10`

Query

| 이름 | 필수 | 설명 |
|---|---|---|
| keyword | 선택 | 제목, 내용에 포함된 키워드 |
| status | 선택 | RECRUITING 또는 CLOSED. 없으면 전체 |
| part | 선택 | PM, DE, BE, FE 중 하나. 작성자의 파트가 같은 글만. 없으면 전체 |
| page | 선택 | 기본 0 |
| size | 선택 | 기본 10 |

Response `200` (최신순 정렬)

```json
{
  "content": [
    {
      "postId": 10,
      "title": "알고리즘 스터디 모집",
      "studyStatus": "RECRUITING",
      "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
      "commentCount": 3,
      "createdAt": "2026-11-08T12:00:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

규칙

- part는 글 작성자의 파트로 거르기 때문에 모집글과 User를 조인해야 해요.
- keyword, status, part는 함께 쓸 수 있고, 함께 쓰면 모두 만족하는 글만 나와요.

에러

- 400 `INVALID_INPUT` (status나 part 값이 올바르지 않을 때)

### 7. 모집글 상세 `GET /api/study-posts/{postId}`

권한: 공개

Response `200`

```json
{
  "postId": 10,
  "title": "알고리즘 스터디 모집",
  "content": "주 2회 백준 풀이...",
  "studyStatus": "RECRUITING",
  "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
  "isAuthor": true,
  "createdAt": "2026-11-08T12:00:00",
  "updatedAt": "2026-11-08T12:00:00"
}
```

규칙

- 로그인하지 않았으면 isAuthor는 false예요.

에러

- 404 `STUDY_POST_NOT_FOUND`

### 8. 모집글 수정 `PATCH /api/study-posts/{postId}`

권한: 작성자

Request (바꿀 필드만 보내요)

```json
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }
```

Response `200`

```json
{ "postId": 10 }
```

에러

- 400 `INVALID_INPUT`
- 403 `NOT_POST_AUTHOR`
- 404 `STUDY_POST_NOT_FOUND`

### 9. 모집글 삭제 `DELETE /api/study-posts/{postId}`

권한: 작성자

Response `204` 본문 없음

규칙

- 글을 삭제하면 달린 댓글도 함께 삭제돼요.

에러

- 403 `NOT_POST_AUTHOR`
- 404 `STUDY_POST_NOT_FOUND`

---

## 7. 댓글 API (10~13번)

댓글이 곧 스터디 신청이에요.

### 10. 댓글 작성 `POST /api/study-posts/{postId}/comments`

권한: 로그인

Request

```json
{ "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요." }
```

Response `201`

```json
{ "commentId": 5 }
```

검증

- content: 1~500자

규칙

- 모집이 마감된(CLOSED) 글에는 댓글을 쓸 수 없어요.

에러

- 400 `INVALID_INPUT`
- 401 `UNAUTHORIZED`
- 404 `STUDY_POST_NOT_FOUND`
- 409 `STUDY_POST_CLOSED`

### 11. 댓글 목록 `GET /api/study-posts/{postId}/comments`

권한: 공개

예: `GET /api/study-posts/10/comments?page=0&size=20`

Response `200` (오래된 순 정렬)

```json
{
  "content": [
    {
      "commentId": 5,
      "content": "안녕하세요! ...",
      "author": { "nickname": "철수", "generation": 13, "part": "FE" },
      "isAuthor": false,
      "createdAt": "2026-11-08T13:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

에러

- 404 `STUDY_POST_NOT_FOUND`

### 12. 댓글 수정 `PATCH /api/comments/{commentId}`

권한: 작성자

Request

```json
{ "content": "수정한 신청 메시지" }
```

Response `200`

```json
{ "commentId": 5 }
```

에러

- 400 `INVALID_INPUT`
- 403 `NOT_COMMENT_AUTHOR`
- 404 `COMMENT_NOT_FOUND`

### 13. 댓글 삭제 `DELETE /api/comments/{commentId}`

권한: 작성자

Response `204` 본문 없음

에러

- 403 `NOT_COMMENT_AUTHOR`
- 404 `COMMENT_NOT_FOUND`

---

## 8. 에러 코드

ErrorCode 상수 이름과 응답 code는 아래와 맞춰요. 응답 code는 3주차 규칙(도메인 prefix + 하이픈 + 일련번호)을 따르고, 기존 enum에 있는 `COMMON-001`(COMMON_BAD_REQUEST), `COMMON-002`(COMMON_INTERNAL_SERVER_ERROR), `BOOK-001`(BOOK_NOT_FOUND)과 겹치지 않게 정했어요.

| HTTP | ErrorCode 상수 이름 | 응답 code | 설명 |
|---|---|---|---|
| 400 | INVALID_INPUT | COMMON-003 | 요청 값 검증 실패 (기수가 1 미만이거나 파트가 목록에 없는 경우 포함) |
| 401 | INVALID_CREDENTIALS | USER-002 | 이메일 또는 비밀번호가 틀려요 |
| 401 | UNAUTHORIZED | COMMON-004 | 토큰이 없거나, 유효하지 않거나, 로그아웃한 토큰이에요 |
| 403 | NOT_POST_AUTHOR | STUDY-002 | 모집글 작성자가 아니에요 |
| 403 | NOT_COMMENT_AUTHOR | COMMENT-002 | 댓글 작성자가 아니에요 |
| 404 | STUDY_POST_NOT_FOUND | STUDY-001 | 모집글이 없어요 |
| 404 | COMMENT_NOT_FOUND | COMMENT-001 | 댓글이 없어요 |
| 409 | EMAIL_ALREADY_EXISTS | USER-001 | 이미 가입된 이메일이에요 |
| 409 | STUDY_POST_CLOSED | STUDY-003 | 모집이 마감된 글에는 댓글을 쓸 수 없어요 |

---

## 9. 프론트엔드 화면

| 화면 | 경로 (예시) | 필요한 동작 |
|---|---|---|
| 회원가입 | /signup | 이메일, 비밀번호, 이름, 닉네임, 기수, 파트(선택 목록) 입력. 입력 검증 메시지, 성공 시 로그인 화면으로 이동 |
| 로그인 | /login | 성공 시 토큰 저장 후 목록으로 이동, 실패 시 에러 메시지 |
| 스터디 목록 | /studies | 키워드 검색, 모집 상태 필터, 파트 필터, 페이지 이동. 각 글에 작성자의 닉네임, 기수, 파트 표시 |
| 스터디 상세 | /studies/:id | 내용 표시, 댓글 목록, 댓글 작성/수정/삭제. 작성자에게만 글 수정, 삭제, 모집 마감 버튼 표시 |
| 스터디 작성/수정 | /studies/new, /studies/:id/edit | 제목, 내용 입력. 수정 화면에서는 모집 상태도 바꿀 수 있어요 |
| 마이페이지 | /mypage | 내 정보 표시: 이메일, 이름, 닉네임, 기수, 파트 |

공통 규칙

- 헤더는 로그인 상태에 따라 달라져요. (로그인/회원가입 버튼 또는 닉네임/마이페이지/로그아웃)
- 로그인이 필요한 화면에 로그인 없이 들어가면 로그인 화면으로 보내요.
- 서버가 내려준 에러 메시지를 화면에 보여줘요. 로딩 중 상태도 표시해요.
- 입력창에는 `label`을 붙이고, 버튼은 `button`, 이동은 링크(`a`)로 만들어요. E2E 테스트가 화면 요소를 역할과 라벨로 찾아요.
- 토큰이 만료되거나 무효가 되어 401이 오면 로그인 화면으로 보내요.
- 디자인은 자유예요. 대신 Tailwind CSS로 만들고, 모바일 화면에서도 깨지지 않게 해요.
- 프론트 개발 서버는 3000번 포트로 열어요. `vite.config.ts`에 `server: { port: 3000, strictPort: true }`를 넣어요. 3주차 CORS 설정이 3000번을 허용하고 있어요.

---

## 10. 구현 메모

명세에 없던 세부 규칙을 구현하면서 정한 내용이에요. 위 명세의 필드와 에러 코드는 바꾸지 않았고, 추가한 테이블과 컬럼도 없어요.

- 페이지 값: `page`는 0 이상, `size`는 1~100이에요. 범위를 벗어나면 400 `INVALID_INPUT`이에요. (목록 6번, 댓글 목록 11번)
- 댓글 목록(11번)의 `size` 기본값은 20이에요.
- 날짜(`createdAt`, `updatedAt`)는 ISO 형식이고, 소수점 아래 초가 붙을 수 있어요. (예: `2026-11-08T12:00:00.123456`)
- 목록 정렬: 모집글은 `createdAt` 내림차순, 댓글은 `createdAt` 오름차순이에요. 같은 시각이면 id 순서로 정해요.
- keyword는 앞뒤 공백을 지우고 제목 또는 내용에 포함된 글을 찾아요. 빈 문자열이면 조건에서 빼요.
- 모집글 수정(8번)에서 보낸 `title`, `content`도 작성(5번)과 같은 길이 규칙을 따르고, 공백만 있으면 400이에요.
- 마감된 글의 기존 댓글은 수정과 삭제가 가능해요. 막는 건 새 댓글 작성(10번)뿐이에요.
- 모집글 삭제(9번)는 댓글을 먼저 지우고 글을 지워요. 엔티티 cascade 대신 service에서 처리해요.
