# API 명세

- 모든 API는 `/api`로 시작한다.
- 인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 보낸다.
- 성공 응답은 `ApiResponse`로 감싼다. 아래 Response 예시는 `data` 안쪽만 적었다. `204`는 본문이 없다.
- 실패 응답은 `ErrorResponse` (`success`, `status`, `code`, `message`, `method`, `path`, `timestamp`)
- 페이지 응답: `content`, `page`, `size`, `totalElements`, `totalPages`
- 작성자 정보: `author { nickname, generation, part }`
- 날짜 필드(`createdAt`, `updatedAt`)는 `yyyy-MM-dd'T'HH:mm:ss`

## 목록

| # | Method | URL | 권한 | 설명 |
|---|---|---|---|---|
| 1 | POST | `/api/auth/signup` | 공개 | 회원가입 |
| 2 | POST | `/api/auth/login` | 공개 | 로그인 |
| 3 | POST | `/api/auth/logout` | 로그인 | 로그아웃 (토큰 무효화) |
| 4 | GET | `/api/users/me` | 로그인 | 내 정보 |
| 5 | POST | `/api/study-posts` | 로그인 | 모집글 작성 |
| 6 | GET | `/api/study-posts` | 공개 | 모집글 목록 (검색·필터·페이징) |
| 7 | GET | `/api/study-posts/{postId}` | 공개 | 모집글 상세 |
| 8 | PATCH | `/api/study-posts/{postId}` | 작성자 | 모집글 수정 |
| 9 | DELETE | `/api/study-posts/{postId}` | 작성자 | 모집글 삭제 (댓글 함께 삭제) |
| 10 | POST | `/api/study-posts/{postId}/comments` | 로그인 | 댓글(신청) 작성 |
| 11 | GET | `/api/study-posts/{postId}/comments` | 공개 | 댓글 목록 |
| 12 | PATCH | `/api/comments/{commentId}` | 작성자 | 댓글 수정 |
| 13 | DELETE | `/api/comments/{commentId}` | 작성자 | 댓글 삭제 |

---

## 인증

### 1. 회원가입 `POST /api/auth/signup`

```json
// Request
{ "email": "user@gmail.com", "password": "password1234", "name": "홍길동",
  "nickname": "길동이", "generation": 14, "part": "BE" }

// Response 201
{ "userId": 1 }
```

- 검증: `email` 형식 / `password` 8자 이상 / `name` 필수 / `nickname` 2~10자 / `generation` 1 이상 정수 / `part` ∈ `PM`,`DE`,`BE`,`FE`
- 비밀번호는 BCrypt로 암호화해 저장
- 에러: 400 `INVALID_INPUT`, 409 `EMAIL_ALREADY_EXISTS`

### 2. 로그인 `POST /api/auth/login`

```json
// Request
{ "email": "user@gmail.com", "password": "password1234" }

// Response 200
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
```

- 에러: 400 `INVALID_INPUT`, 401 `INVALID_CREDENTIALS`

### 3. 로그아웃 `POST /api/auth/logout`

- Response 204 (본문 없음)
- 요청에 쓴 토큰을 `blacklisted_token` 테이블에 만료 시각과 함께 저장한다. 이후 그 토큰으로 요청하면 401.
- 에러: 401 `UNAUTHORIZED`

### 4. 내 정보 `GET /api/users/me`

```json
// Response 200
{ "userId": 1, "email": "user@gmail.com", "name": "홍길동", "nickname": "길동이",
  "generation": 14, "part": "BE" }
```

- 비밀번호는 응답에 넣지 않는다.
- 에러: 401 `UNAUTHORIZED`

---

## 스터디 모집글

### 5. 작성 `POST /api/study-posts`

```json
// Request
{ "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }

// Response 201
{ "postId": 10 }
```

- 검증: `title` 1~50자 / `content` 1~2000자
- 처음 상태는 `RECRUITING`
- 에러: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`

### 6. 목록 `GET /api/study-posts?keyword=&status=&part=&page=0&size=10`

| Query | 필수 | 설명 |
|---|---|---|
| keyword | X | 제목 또는 내용에 포함 |
| status | X | `RECRUITING` / `CLOSED` |
| part | X | 작성자 파트 (`PM`/`DE`/`BE`/`FE`) — User 조인 |
| page | X | 기본 0 |
| size | X | 기본 10 |

```json
// Response 200 (최신순)
{
  "content": [
    { "postId": 10, "title": "알고리즘 스터디 모집", "studyStatus": "RECRUITING",
      "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
      "commentCount": 3, "createdAt": "2026-11-08T12:00:00" }
  ],
  "page": 0, "size": 10, "totalElements": 1, "totalPages": 1
}
```

- 조건은 모두 AND
- 에러: 400 `INVALID_INPUT` (status·part 값이 올바르지 않을 때)

### 7. 상세 `GET /api/study-posts/{postId}`

```json
// Response 200
{
  "postId": 10, "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이...",
  "studyStatus": "RECRUITING",
  "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
  "isAuthor": true,
  "createdAt": "2026-11-08T12:00:00", "updatedAt": "2026-11-08T12:00:00"
}
```

- 비로그인이면 `isAuthor`는 `false`
- 에러: 404 `STUDY_POST_NOT_FOUND`

### 8. 수정 `PATCH /api/study-posts/{postId}`

```json
// Request (바꿀 필드만)
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }

// Response 200
{ "postId": 10 }
```

- 검증: 보낸 경우 `title` 1~50자 / `content` 1~2000자 / `studyStatus` ∈ `RECRUITING`,`CLOSED`
- 에러: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 403 `NOT_POST_AUTHOR`, 404 `STUDY_POST_NOT_FOUND`

### 9. 삭제 `DELETE /api/study-posts/{postId}`

- Response 204. 달린 댓글도 함께 삭제
- 에러: 401 `UNAUTHORIZED`, 403 `NOT_POST_AUTHOR`, 404 `STUDY_POST_NOT_FOUND`

---

## 댓글 (= 스터디 신청)

### 10. 작성 `POST /api/study-posts/{postId}/comments`

```json
// Request
{ "content": "안녕하세요! 참여하고 싶어요." }

// Response 201
{ "commentId": 5 }
```

- 검증: `content` 1~500자
- `CLOSED` 글에는 작성 불가
- 에러: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 404 `STUDY_POST_NOT_FOUND`, 409 `STUDY_POST_CLOSED`

### 11. 목록 `GET /api/study-posts/{postId}/comments?page=0&size=20`

```json
// Response 200 (오래된 순)
{
  "content": [
    { "commentId": 5, "content": "안녕하세요! ...",
      "author": { "nickname": "철수", "generation": 13, "part": "FE" },
      "isAuthor": false, "createdAt": "2026-11-08T13:00:00" }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

- 에러: 404 `STUDY_POST_NOT_FOUND`

### 12. 수정 `PATCH /api/comments/{commentId}`

```json
// Request
{ "content": "수정한 신청 메시지" }

// Response 200
{ "commentId": 5 }
```

- 에러: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 403 `NOT_COMMENT_AUTHOR`, 404 `COMMENT_NOT_FOUND`

### 13. 삭제 `DELETE /api/comments/{commentId}`

- Response 204
- 에러: 401 `UNAUTHORIZED`, 403 `NOT_COMMENT_AUTHOR`, 404 `COMMENT_NOT_FOUND`

---

## 에러 코드

| HTTP | ErrorCode | code | message |
|---|---|---|---|
| 400 | `INVALID_INPUT` | `COMMON-003` | 입력값이 올바르지 않아요. (필드 검증 메시지로 대체될 수 있음) |
| 401 | `UNAUTHORIZED` | `COMMON-004` | 로그인이 필요해요. |
| 500 | `COMMON_INTERNAL_SERVER_ERROR` | `COMMON-002` | 서버 내부 오류가 발생했어요. |
| 409 | `EMAIL_ALREADY_EXISTS` | `USER-001` | 이미 가입된 이메일이에요. |
| 401 | `INVALID_CREDENTIALS` | `USER-002` | 이메일 또는 비밀번호가 올바르지 않아요. |
| 404 | `USER_NOT_FOUND` | `USER-003` | 존재하지 않는 회원이에요. |
| 404 | `STUDY_POST_NOT_FOUND` | `STUDY-001` | 존재하지 않는 스터디 모집글이에요. |
| 403 | `NOT_POST_AUTHOR` | `STUDY-002` | 모집글 작성자만 할 수 있어요. |
| 409 | `STUDY_POST_CLOSED` | `STUDY-003` | 모집이 마감된 글에는 댓글을 쓸 수 없어요. |
| 404 | `COMMENT_NOT_FOUND` | `COMMENT-001` | 존재하지 않는 댓글이에요. |
| 403 | `NOT_COMMENT_AUTHOR` | `COMMENT-002` | 댓글 작성자만 할 수 있어요. |

## 명세에 추가한 것

- 테이블 `blacklisted_token` (로그아웃한 토큰 저장)
- ErrorCode `USER_NOT_FOUND` (토큰은 유효하지만 회원이 삭제된 경우)
