# API 명세

코테이토 스터디 매칭 플랫폼의 API 13개를 정리한 문서예요.
백엔드를 만들거나 프론트에서 API를 부를 때 이 문서를 기준으로 해요.

## 공통 규칙

- 모든 API 주소는 `/api`로 시작해요.
- 인증이 필요한 API는 요청 헤더에 `Authorization: Bearer {accessToken}`을 담아 보내요.
- 권한
  - **공개**: 토큰 없이 호출할 수 있어요.
  - **로그인**: 토큰이 필요해요.
  - **작성자**: 토큰이 필요하고, 작성자 본인이어야 해요.
- 토큰은 `accessToken` 하나만 써요.

### 성공 응답
3주차 공통 응답(`ApiResponse`)으로 감싸요. **아래 예시는 `data` 안쪽만 적은 거예요.**
```json
{ "success": true, "status": 200, "data": { ... }, "timestamp": "2026-11-08 12:00:00" }
```
본문이 없는 `204` 응답은 감싸지 않고 그대로 내려줘요.

### 실패 응답
모든 API에서 3주차 `ErrorResponse` 형식으로 내려줘요.
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

### 페이지 응답
목록 API는 아래 형식으로 내려줘요.
```json
{ "content": [ ... ], "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
```

### 작성자 정보 (`author`)
```json
{ "nickname": "길동이", "generation": 14, "part": "BE" }
```

### 값 목록
| 필드 | 가능한 값 |
|---|---|
| `part` | `PM`(기획), `DE`(디자인), `BE`(백엔드), `FE`(프론트엔드) |
| `studyStatus` | `RECRUITING`(모집 중), `CLOSED`(모집 마감) |

## API 목록

| # | Method | URL | 권한 | 설명 |
|---|---|---|---|---|
| 1 | POST | `/api/auth/signup` | 공개 | 회원가입 (기수, 파트 포함) |
| 2 | POST | `/api/auth/login` | 공개 | 로그인 (토큰 발급) |
| 3 | POST | `/api/auth/logout` | 로그인 | 로그아웃 (토큰 무효화) |
| 4 | GET | `/api/users/me` | 로그인 | 내 정보 조회 (기수, 파트 포함) |
| 5 | POST | `/api/study-posts` | 로그인 | 모집글 작성 |
| 6 | GET | `/api/study-posts` | 공개 | 모집글 목록 (검색, 모집 상태·파트 필터, 페이징) |
| 7 | GET | `/api/study-posts/{postId}` | 공개 | 모집글 상세 |
| 8 | PATCH | `/api/study-posts/{postId}` | 작성자 | 모집글 수정 (모집 상태 변경 포함) |
| 9 | DELETE | `/api/study-posts/{postId}` | 작성자 | 모집글 삭제 (댓글도 함께 삭제) |
| 10 | POST | `/api/study-posts/{postId}/comments` | 로그인 | 댓글(신청) 작성 |
| 11 | GET | `/api/study-posts/{postId}/comments` | 공개 | 댓글 목록 |
| 12 | PATCH | `/api/comments/{commentId}` | 작성자 | 댓글 수정 |
| 13 | DELETE | `/api/comments/{commentId}` | 작성자 | 댓글 삭제 |

---

## 인증

### 1. 회원가입 `POST /api/auth/signup` (공개)
**Request**
```json
{ "email": "user@gmail.com", "password": "password1234", "name": "홍길동",
  "nickname": "길동이", "generation": 14, "part": "BE" }
```
**Response `201`**
```json
{ "userId": 1 }
```
- 검증: `email` 형식 / `password` 8자 이상 / `nickname` 2~10자 / `generation` 1 이상의 정수 / `part`는 `PM`, `DE`, `BE`, `FE` 중 하나
- 비밀번호는 BCrypt로 암호화해서 저장해요.
- 에러: `400 INVALID_INPUT`, `409 EMAIL_ALREADY_EXISTS`

### 2. 로그인 `POST /api/auth/login` (공개)
**Request**
```json
{ "email": "user@gmail.com", "password": "password1234" }
```
**Response `200`**
```json
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
```
- 에러: `401 INVALID_CREDENTIALS`

### 3. 로그아웃 `POST /api/auth/logout` (로그인)
**Response `204`** 본문 없음
- 서버는 사용한 토큰을 만료 시각까지 무효 처리해요. (저장 방식은 자유: DB, 인메모리 등)
- 로그아웃한 토큰으로 다시 요청하면 `401`을 내려줘요.
- 프론트는 저장해둔 토큰을 지우고 로그인 화면으로 이동해요.
- 에러: `401 UNAUTHORIZED`

### 4. 내 정보 조회 `GET /api/users/me` (로그인)
**Response `200`**
```json
{
  "userId": 1, "email": "user@gmail.com", "name": "홍길동", "nickname": "길동이",
  "generation": 14, "part": "BE"
}
```
- 비밀번호는 절대 응답에 넣지 않아요.
- 에러: `401 UNAUTHORIZED`

---

## 스터디 모집글

### 5. 모집글 작성 `POST /api/study-posts` (로그인)
**Request**
```json
{ "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }
```
**Response `201`**
```json
{ "postId": 10 }
```
- 검증: `title` 1~50자 / `content` 1~2000자
- 처음 만들면 `studyStatus`는 `RECRUITING`이에요.
- 에러: `400 INVALID_INPUT`, `401 UNAUTHORIZED`

### 6. 모집글 목록 `GET /api/study-posts` (공개)
`/api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10`

| Query | 필수 | 설명 |
|---|---|---|
| `keyword` | 선택 | 제목, 내용에 포함된 키워드 |
| `status` | 선택 | `RECRUITING` 또는 `CLOSED`. 없으면 전체 |
| `part` | 선택 | `PM`, `DE`, `BE`, `FE` 중 하나. **작성자의 파트**가 같은 글만. 없으면 전체 |
| `page` | 선택 | 기본 0 |
| `size` | 선택 | 기본 10 |

**Response `200`** 최신순 정렬
```json
{
  "content": [
    { "postId": 10, "title": "알고리즘 스터디 모집", "studyStatus": "RECRUITING",
      "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
      "commentCount": 3, "createdAt": "2026-11-08T12:00:00" }
  ],
  "page": 0, "size": 10, "totalElements": 1, "totalPages": 1
}
```
- `part`는 글 작성자의 파트로 거르기 때문에 모집글과 User를 조인해야 해요.
- `keyword`, `status`, `part`는 함께 쓸 수 있고, 함께 쓰면 모두 만족하는 글만 나와요.
- 에러: `400 INVALID_INPUT` (`status`나 `part` 값이 올바르지 않을 때)

### 7. 모집글 상세 `GET /api/study-posts/{postId}` (공개)
**Response `200`**
```json
{
  "postId": 10, "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이...",
  "studyStatus": "RECRUITING",
  "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
  "isAuthor": true,
  "createdAt": "2026-11-08T12:00:00", "updatedAt": "2026-11-08T12:00:00"
}
```
- 로그인하지 않았으면 `isAuthor`는 `false`예요.
- 에러: `404 STUDY_POST_NOT_FOUND`

### 8. 모집글 수정 `PATCH /api/study-posts/{postId}` (작성자)
**Request** 바꿀 필드만 보내요
```json
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }
```
**Response `200`**
```json
{ "postId": 10 }
```
- 모집 상태는 `RECRUITING`과 `CLOSED` 사이를 자유롭게 바꿀 수 있어요.
- 에러: `400 INVALID_INPUT`, `403 NOT_POST_AUTHOR`, `404 STUDY_POST_NOT_FOUND`

### 9. 모집글 삭제 `DELETE /api/study-posts/{postId}` (작성자)
**Response `204`** 본문 없음
- 글을 삭제하면 달린 댓글도 함께 삭제돼요.
- 에러: `403 NOT_POST_AUTHOR`, `404 STUDY_POST_NOT_FOUND`

---

## 댓글 (= 스터디 신청)

### 10. 댓글 작성 `POST /api/study-posts/{postId}/comments` (로그인)
**Request**
```json
{ "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요." }
```
**Response `201`**
```json
{ "commentId": 5 }
```
- 검증: `content` 1~500자
- 모집이 마감된(`CLOSED`) 글에는 댓글을 쓸 수 없어요.
- 같은 사람이 댓글을 여러 번 달아도 돼요.
- 에러: `400 INVALID_INPUT`, `401 UNAUTHORIZED`, `404 STUDY_POST_NOT_FOUND`, `409 STUDY_POST_CLOSED`

### 11. 댓글 목록 `GET /api/study-posts/{postId}/comments` (공개)
`/api/study-posts/{postId}/comments?page=0&size=20`

**Response `200`** 오래된 순 정렬
```json
{
  "content": [
    { "commentId": 5, "content": "안녕하세요! ...",
      "author": { "nickname": "철수", "generation": 13, "part": "FE" },
      "isAuthor": false, "createdAt": "2026-11-08T13:00:00" }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```
- 에러: `404 STUDY_POST_NOT_FOUND`

### 12. 댓글 수정 `PATCH /api/comments/{commentId}` (작성자)
**Request**
```json
{ "content": "수정한 신청 메시지" }
```
**Response `200`**
```json
{ "commentId": 5 }
```
- 에러: `400 INVALID_INPUT`, `403 NOT_COMMENT_AUTHOR`, `404 COMMENT_NOT_FOUND`

### 13. 댓글 삭제 `DELETE /api/comments/{commentId}` (작성자)
**Response `204`** 본문 없음
- 에러: `403 NOT_COMMENT_AUTHOR`, `404 COMMENT_NOT_FOUND`

---

## 에러 코드

`ErrorCode` enum 상수 이름은 아래와 맞추고, 응답 `code`는 `도메인-일련번호` 형식이에요.

| ErrorCode 상수 | HTTP | 응답 code | 설명 |
|---|---|---|---|
| `INVALID_INPUT` | 400 | `COMMON-003` | 요청 값 검증 실패 (기수가 1 미만이거나 파트가 목록에 없는 경우 포함) |
| `UNAUTHORIZED` | 401 | `COMMON-004` | 토큰이 없거나, 유효하지 않거나, 로그아웃한 토큰이에요 |
| `EMAIL_ALREADY_EXISTS` | 409 | `USER-001` | 이미 가입된 이메일이에요 |
| `INVALID_CREDENTIALS` | 401 | `USER-002` | 이메일 또는 비밀번호가 틀려요 |
| `STUDY_POST_NOT_FOUND` | 404 | `STUDY-001` | 존재하지 않는 스터디 모집글이에요 |
| `NOT_POST_AUTHOR` | 403 | `STUDY-002` | 모집글 작성자가 아니에요 |
| `STUDY_POST_CLOSED` | 409 | `STUDY-003` | 모집이 마감된 글에는 댓글을 쓸 수 없어요 |
| `COMMENT_NOT_FOUND` | 404 | `COMMENT-001` | 존재하지 않는 댓글이에요 |
| `NOT_COMMENT_AUTHOR` | 403 | `COMMENT-002` | 댓글 작성자가 아니에요 |

`COMMON-001`(잘못된 요청), `COMMON-002`(서버 내부 오류), `BOOK-001`은 3주차부터 있던 코드예요.

## 명세 변경 기록

명세에 없는 필드나 테이블을 추가하면 여기에 적어요. (명세에 있는 필드 이름과 의미는 바꾸지 않아요)

| 날짜 | 변경 내용 | 이유 |
|---|---|---|
| | | |
