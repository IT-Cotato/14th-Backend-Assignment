# API 명세: 코테이토 스터디 매칭 플랫폼

- 서버 주소(로컬): `http://localhost:8080`
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- ERD: `docs/erd/` 참고

---

## 1. 공통 규칙

### 1-1. 주소와 인증

- 모든 API 주소는 `/api` 로 시작한다.
- 인증이 필요한 API는 요청 헤더에 토큰을 담는다.

      Authorization: Bearer {accessToken}

- 토큰은 `accessToken` 하나만 쓴다. (refresh token 없음)

### 1-2. 권한

| 권한 | 의미 |
| --- | --- |
| 공개 | 토큰 없이 호출할 수 있다 |
| 로그인 | 유효한 토큰이 필요하다. 없거나, 유효하지 않거나, 로그아웃한 토큰이면 `401 UNAUTHORIZED` |
| 작성자 | 로그인 + 대상(모집글, 댓글)의 작성자 본인이어야 한다. 아니면 `403` |

- 공개 API에 토큰을 함께 보내면 서버는 토큰으로 사용자를 식별한다. (`isAuthor` 계산용)

### 1-3. 성공 응답

공통 응답(`ApiResponse`)으로 감싼다. **아래 각 API의 응답 예시는 `data` 안쪽만 적는다.**

```json
{
  "success": true,
  "status": 200,
  "data": { ... },
  "timestamp": "2026-11-08 12:00:00"
}
```

- 본문이 없는 `204 No Content` 응답은 감싸지 않고 본문 없이 내려준다.

### 1-4. 실패 응답

모든 API가 `ErrorResponse` 형식으로 내려준다.

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

### 1-5. 페이지 응답

목록 API는 아래 형식으로 내려준다.

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

- `page` 는 0부터 시작한다.

### 1-6. 작성자 정보 (`author`)

모집글과 댓글의 작성자는 `author` 객체로 내려준다.

```json
{ "nickname": "길동이", "generation": 14, "part": "BE" }
```

### 1-7. Enum 값

| Enum | 값 | 의미 |
| --- | --- | --- |
| `part` | `PM`, `DE`, `BE`, `FE` | 기획, 디자인, 백엔드, 프론트엔드 |
| `studyStatus` | `RECRUITING`, `CLOSED` | 모집 중, 모집 마감 |

- DB에는 문자열로 저장한다.
- 응답의 날짜 필드(`createdAt`, `updatedAt`)는 `yyyy-MM-ddTHH:mm:ss` 형식이다.

### 1-8. 세부 규칙 (명세에 없는 부분은 쉬운 쪽으로)

- 모집 상태는 `RECRUITING` 과 `CLOSED` 사이를 자유롭게 바꿀 수 있다.
- 같은 사람이 한 글에 댓글을 여러 번 달 수 있다.
- 로그아웃한 토큰은 `token_blacklist` 테이블에 저장하고, 원래 만료 시각까지 무효로 처리한다.

---

## 2. API 목록 (13개)

| # | Method | URL | 권한 | 설명 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/api/auth/signup` | 공개 | 회원가입 (기수, 파트 포함) |
| 2 | POST | `/api/auth/login` | 공개 | 로그인 (토큰 발급) |
| 3 | POST | `/api/auth/logout` | 로그인 | 로그아웃 (토큰 무효화) |
| 4 | GET | `/api/users/me` | 로그인 | 내 정보 조회 |
| 5 | POST | `/api/study-posts` | 로그인 | 모집글 작성 |
| 6 | GET | `/api/study-posts` | 공개 | 모집글 목록 (검색, 필터, 페이징) |
| 7 | GET | `/api/study-posts/{postId}` | 공개 | 모집글 상세 |
| 8 | PATCH | `/api/study-posts/{postId}` | 작성자 | 모집글 수정 (모집 상태 변경 포함) |
| 9 | DELETE | `/api/study-posts/{postId}` | 작성자 | 모집글 삭제 (댓글도 함께 삭제) |
| 10 | POST | `/api/study-posts/{postId}/comments` | 로그인 | 댓글(신청) 작성 |
| 11 | GET | `/api/study-posts/{postId}/comments` | 공개 | 댓글 목록 |
| 12 | PATCH | `/api/comments/{commentId}` | 작성자 | 댓글 수정 |
| 13 | DELETE | `/api/comments/{commentId}` | 작성자 | 댓글 삭제 |

---

## 3. 인증

### 1. 회원가입 `POST /api/auth/signup` (공개)

**Request**

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

| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| `email` | string | O | 이메일 형식 |
| `password` | string | O | 8자 이상 |
| `name` | string | O | 1~50자 |
| `nickname` | string | O | 2~10자 |
| `generation` | number | O | 1 이상의 정수 |
| `part` | string | O | `PM`, `DE`, `BE`, `FE` 중 하나 |

**Response `201 Created`**

```json
{ "userId": 1 }
```

**규칙**

- 비밀번호는 BCrypt로 암호화해서 저장한다.
- 기수와 파트는 본인이 입력한 값을 그대로 저장한다. (실제 소속 검증 없음)

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 검증 실패 (기수 1 미만, 파트가 목록에 없음 포함) |
| 409 | `EMAIL_ALREADY_EXISTS` | 이미 가입된 이메일 |

### 2. 로그인 `POST /api/auth/login` (공개)

**Request**

```json
{ "email": "user@gmail.com", "password": "password1234" }
```

**Response `200 OK`**

```json
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
```

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 이메일이나 비밀번호가 비어 있음 |
| 401 | `INVALID_CREDENTIALS` | 이메일이 없거나 비밀번호가 틀림 (둘을 구분하지 않는다) |

### 3. 로그아웃 `POST /api/auth/logout` (로그인)

**Response `204 No Content`** 본문 없음

**규칙**

- 요청에 쓴 토큰을 `token_blacklist` 에 저장하고, 토큰의 원래 만료 시각까지 무효로 처리한다.
- 로그아웃한 토큰으로 다시 요청하면 `401 UNAUTHORIZED` 를 내려준다.
- 프론트는 저장해둔 토큰을 지우고 로그인 화면으로 이동한다.

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 토큰 없음, 유효하지 않음, 이미 로그아웃한 토큰 |

### 4. 내 정보 조회 `GET /api/users/me` (로그인)

**Response `200 OK`**

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

**규칙**

- 비밀번호는 절대 응답에 넣지 않는다.

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 토큰 없음, 유효하지 않음, 로그아웃한 토큰 |

---

## 4. 스터디 모집글

### 5. 모집글 작성 `POST /api/study-posts` (로그인)

**Request**

```json
{ "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }
```

| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| `title` | string | O | 1~50자 |
| `content` | string | O | 1~2000자 |

**Response `201 Created`**

```json
{ "postId": 10 }
```

**규칙**

- 처음 만들면 `studyStatus` 는 `RECRUITING` 이다.

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 검증 실패 |
| 401 | `UNAUTHORIZED` | 로그인하지 않음 |

### 6. 모집글 목록 `GET /api/study-posts` (공개)

예: `GET /api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10`

**Query**

| 이름 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `keyword` | X | 없음 | 제목 또는 내용에 포함된 키워드 |
| `status` | X | 없음(전체) | `RECRUITING` 또는 `CLOSED` |
| `part` | X | 없음(전체) | `PM`, `DE`, `BE`, `FE`. **작성자의 파트**가 같은 글만 |
| `page` | X | 0 | 페이지 번호 (0부터) |
| `size` | X | 10 | 페이지 크기 (1~100) |

**Response `200 OK`** 최신순(`createdAt` 내림차순) 정렬

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

**규칙**

- `part` 는 글 작성자의 파트로 거르므로 모집글과 `users` 를 조인한다.
- `keyword`, `status`, `part` 는 함께 쓸 수 있고, 함께 쓰면 모두 만족하는 글만 나온다.

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | `status` 나 `part` 값이 올바르지 않음, `page` 가 음수, `size` 가 1~100을 벗어남 |

### 7. 모집글 상세 `GET /api/study-posts/{postId}` (공개)

**Response `200 OK`**

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

**규칙**

- 로그인하지 않았으면 `isAuthor` 는 `false` 다.

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 404 | `STUDY_POST_NOT_FOUND` | 글이 없음 |

### 8. 모집글 수정 `PATCH /api/study-posts/{postId}` (작성자)

**Request** 바꿀 필드만 보낸다.

```json
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }
```

| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| `title` | string | X | 보내면 1~50자 |
| `content` | string | X | 보내면 1~2000자 |
| `studyStatus` | string | X | 보내면 `RECRUITING` 또는 `CLOSED` |

**Response `200 OK`**

```json
{ "postId": 10 }
```

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 검증 실패 |
| 401 | `UNAUTHORIZED` | 로그인하지 않음 |
| 403 | `NOT_POST_AUTHOR` | 작성자가 아님 |
| 404 | `STUDY_POST_NOT_FOUND` | 글이 없음 |

### 9. 모집글 삭제 `DELETE /api/study-posts/{postId}` (작성자)

**Response `204 No Content`** 본문 없음

**규칙**

- 글을 삭제하면 달린 댓글도 함께 삭제된다.

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 로그인하지 않음 |
| 403 | `NOT_POST_AUTHOR` | 작성자가 아님 |
| 404 | `STUDY_POST_NOT_FOUND` | 글이 없음 |

---

## 5. 댓글 (= 스터디 신청)

### 10. 댓글 작성 `POST /api/study-posts/{postId}/comments` (로그인)

**Request**

```json
{ "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요." }
```

| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| `content` | string | O | 1~500자 |

**Response `201 Created`**

```json
{ "commentId": 5 }
```

**규칙**

- 모집이 마감된(`CLOSED`) 글에는 댓글을 쓸 수 없다.

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 검증 실패 |
| 401 | `UNAUTHORIZED` | 로그인하지 않음 |
| 404 | `STUDY_POST_NOT_FOUND` | 글이 없음 |
| 409 | `STUDY_POST_CLOSED` | 마감된 글 |

### 11. 댓글 목록 `GET /api/study-posts/{postId}/comments` (공개)

예: `GET /api/study-posts/10/comments?page=0&size=20`

**Query**

| 이름 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `page` | X | 0 | 페이지 번호 (0부터) |
| `size` | X | 20 | 페이지 크기 (1~100) |

**Response `200 OK`** 오래된 순(`createdAt` 오름차순) 정렬

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

**규칙**

- 로그인하지 않았으면 `isAuthor` 는 `false` 다.

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | `page` 가 음수, `size` 가 1~100을 벗어남 |
| 404 | `STUDY_POST_NOT_FOUND` | 글이 없음 |

### 12. 댓글 수정 `PATCH /api/comments/{commentId}` (작성자)

**Request**

```json
{ "content": "수정한 신청 메시지" }
```

| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| `content` | string | O | 1~500자 |

**Response `200 OK`**

```json
{ "commentId": 5 }
```

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 검증 실패 |
| 401 | `UNAUTHORIZED` | 로그인하지 않음 |
| 403 | `NOT_COMMENT_AUTHOR` | 작성자가 아님 |
| 404 | `COMMENT_NOT_FOUND` | 댓글이 없음 |

### 13. 댓글 삭제 `DELETE /api/comments/{commentId}` (작성자)

**Response `204 No Content`** 본문 없음

**에러**

| HTTP | ErrorCode | 상황 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 로그인하지 않음 |
| 403 | `NOT_COMMENT_AUTHOR` | 작성자가 아님 |
| 404 | `COMMENT_NOT_FOUND` | 댓글이 없음 |

---

## 6. 에러 코드

기존 `ErrorCode` enum(`COMMON-001`, `COMMON-002`, `BOOK-001`)에 이어서 추가한다.

| HTTP | ErrorCode 상수 | code | 메시지 |
| --- | --- | --- | --- |
| 400 | `INVALID_INPUT` | `COMMON-003` | 입력값이 올바르지 않아요. |
| 401 | `UNAUTHORIZED` | `COMMON-004` | 로그인이 필요해요. |
| 409 | `EMAIL_ALREADY_EXISTS` | `USER-001` | 이미 가입된 이메일이에요. |
| 401 | `INVALID_CREDENTIALS` | `USER-002` | 이메일 또는 비밀번호가 올바르지 않아요. |
| 404 | `STUDY_POST_NOT_FOUND` | `STUDY-001` | 존재하지 않는 스터디 모집글이에요. |
| 403 | `NOT_POST_AUTHOR` | `STUDY-002` | 모집글 작성자만 할 수 있어요. |
| 409 | `STUDY_POST_CLOSED` | `STUDY-003` | 모집이 마감된 글에는 댓글을 쓸 수 없어요. |
| 404 | `COMMENT_NOT_FOUND` | `COMMENT-001` | 존재하지 않는 댓글이에요. |
| 403 | `NOT_COMMENT_AUTHOR` | `COMMENT-002` | 댓글 작성자만 할 수 있어요. |

---

## 7. 데이터 모델

| 테이블 | 컬럼 | 비고 |
| --- | --- | --- |
| `users` | `id`, `email`(UK), `password`, `name`, `nickname`, `generation`, `part`, `created_at`, `updated_at` | `generation` 1 이상, `part` 는 `PM`/`DE`/`BE`/`FE` |
| `study_post` | `id`, `user_id`(FK), `title`, `content`, `study_status`, `created_at`, `updated_at` | `study_status` 는 `RECRUITING`/`CLOSED` |
| `study_post_comment` | `id`, `study_post_id`(FK), `user_id`(FK), `content`, `created_at`, `updated_at` | 댓글 = 스터디 신청 |
| `token_blacklist` | `id`, `token`(UK), `expires_at`, `created_at`, `updated_at` | **추가한 테이블.** 로그아웃한 토큰 저장 |

- `User` 테이블 이름은 `users` 로 한다. `user` 는 DB에 따라 예약어라서 피한다. 컬럼 이름과 의미는 과제 명세 그대로다.
- 연관관계는 모두 `LAZY`, Enum은 문자열로 저장한다.
- 모집글 삭제 시 댓글 삭제는 애플리케이션(서비스)에서 처리한다.

### 추가한 테이블: `token_blacklist`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | |
| `token` | VARCHAR(512) UK | 로그아웃한 accessToken |
| `expires_at` | DATETIME | 토큰의 원래 만료 시각 |
| `created_at`, `updated_at` | DATETIME | `BaseEntity` |

- 추가한 이유: 명세의 "만료 시각까지 무효 처리"를 서버 재시작 뒤에도 유지하기 위해서다. 인메모리 방식은 재시작하면 블랙리스트가 사라진다.
- 만료된 행을 지우는 스케줄러는 두지 않는다. 만료된 토큰은 JWT 검증 단계에서 이미 거절된다.
