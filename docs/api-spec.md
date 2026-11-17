# API 명세

코테이토 스터디 매칭 플랫폼 백엔드 API 명세다.

## 공통 규칙

- 모든 API의 주소는 `/api`로 시작한다.
- 인증이 필요한 API는 요청 헤더에 `Authorization: Bearer {accessToken}`을 담아 보낸다.
- 권한은 세 가지다.
  - **공개**: 인증 불필요
  - **로그인**: 유효한 토큰 필요
  - **작성자**: 로그인 + 대상 글·댓글의 작성자 본인

### 성공 응답

성공 응답은 공통 응답(`ApiResponse`)으로 감싼다. 아래 각 API의 `Response`는 `data` 안쪽 값만 적은 것이다.
본문이 없는 `204` 응답은 감싸지 않는다.

```json
// 예: 회원가입 성공 (201)
{
  "success": true,
  "status": 201,
  "data": { "userId": 1 },
  "timestamp": "2026-11-08 12:00:00"
}
```

- `data`가 없으면 `data` 필드는 응답에서 빠진다.
- 공통 `timestamp`는 `2026-11-08 12:00:00` 형식이고, `createdAt`·`updatedAt` 같은 도메인 필드는 `2026-11-08T12:00:00` 형식이다.

### 실패 응답

모든 실패는 `ErrorResponse` 형식으로 내려준다.

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

- `code`는 `도메인 prefix + 하이픈 + 일련번호` 형식이다.
- 비밀번호, 토큰, 스택 트레이스는 응답에 포함하지 않는다.

### 페이지 응답

목록 API는 다음 필드를 내려준다.

| 필드 | 설명 |
|---|---|
| `content` | 현재 페이지의 항목 목록 |
| `page` | 현재 페이지 번호 (0부터) |
| `size` | 페이지 크기 |
| `totalElements` | 전체 항목 수 |
| `totalPages` | 전체 페이지 수 |

### 작성자 정보

작성자는 `author` 객체로 내려준다.

```json
{ "nickname": "길동이", "generation": 14, "part": "BE" }
```

### 명세에 없는 세부 규칙

명세에 없는 규칙은 단순한 쪽으로 구현한다.

- 모집 상태는 `RECRUITING`과 `CLOSED` 사이를 자유롭게 바꿀 수 있다.
- 같은 사람이 같은 글에 댓글을 여러 번 달 수 있다.
- 토큰은 `accessToken` 하나만 사용한다.

## API 목록 (13개)

| # | Method | URL | 권한 | 설명 |
|---|---|---|---|---|
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

## 인증 (1~4번)

### 1. 회원가입 `POST /api/auth/signup`

```json
// Request
{
  "email": "user@gmail.com",
  "password": "password1234",
  "name": "홍길동",
  "nickname": "길동이",
  "generation": 14,
  "part": "BE"
}

// Response 201
{ "userId": 1 }
```

- 검증: `email` 형식 / `password` 8자 이상 72자 이하 / `nickname` 2~10자 / `generation` 1 이상의 정수 / `part`는 `PM`, `DE`, `BE`, `FE` 중 하나
- 규칙: 비밀번호는 BCrypt로 암호화해 저장한다. BCrypt가 72바이트까지만 처리하므로 상한을 둔다. 기수와 파트는 본인이 입력하는 값이며 별도로 검증하지 않는다.
- 에러: `400 INVALID_INPUT`, `409 EMAIL_ALREADY_EXISTS`

### 2. 로그인 `POST /api/auth/login`

```json
// Request
{ "email": "user@gmail.com", "password": "password1234" }

// Response 200
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
```

- 규칙: 이메일이 없거나 비밀번호가 틀리면 같은 에러를 내려준다. (어느 쪽이 틀렸는지 알려주지 않는다.)
- 에러: `401 INVALID_CREDENTIALS`

### 3. 로그아웃 `POST /api/auth/logout`

```
// Response 204 (본문 없음)
```

- 규칙
  - 서버는 사용한 토큰을 만료 시각까지 무효 처리한다.
  - 로그아웃한 토큰으로 다시 요청하면 `401`을 내려준다.
  - 프론트는 저장해 둔 토큰을 지우고 로그인 화면으로 이동한다.
- 저장 방식: DB 테이블 (아래 "추가 설계" 참고)
- 에러: `401 UNAUTHORIZED`

### 4. 내 정보 조회 `GET /api/users/me`

```json
// Response 200
{
  "userId": 1,
  "email": "user@gmail.com",
  "name": "홍길동",
  "nickname": "길동이",
  "generation": 14,
  "part": "BE"
}
```

- 규칙: 비밀번호는 응답에 절대 넣지 않는다.
- 에러: `401 UNAUTHORIZED`

## 스터디 모집글 (5~9번)

### 5. 모집글 작성 `POST /api/study-posts`

```json
// Request
{ "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }

// Response 201
{ "postId": 10 }
```

- 검증: `title` 1~50자 / `content` 1~2000자
- 규칙: 처음 만들면 `studyStatus`는 `RECRUITING`이다.
- 에러: `400 INVALID_INPUT`, `401 UNAUTHORIZED`

### 6. 모집글 목록 `GET /api/study-posts`

`GET /api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10`

| 쿼리 | 필수 | 설명 |
|---|---|---|
| `keyword` | 선택 | 제목, 내용에 포함된 키워드 |
| `status` | 선택 | `RECRUITING` 또는 `CLOSED`. 없으면 전체 |
| `part` | 선택 | `PM`, `DE`, `BE`, `FE` 중 하나. 작성자의 파트가 같은 글만. 없으면 전체 |
| `page` | 선택 | 기본 0 |
| `size` | 선택 | 기본 10 |

```json
// Response 200 (최신순 정렬)
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

- 규칙
  - `part`는 글 작성자의 파트로 거르므로 모집글과 User를 조인한다.
  - `keyword`, `status`, `part`는 함께 쓸 수 있고, 함께 쓰면 모두 만족하는 글만 나온다.
- 에러: `400 INVALID_INPUT` (`status`나 `part` 값이 올바르지 않을 때)

### 7. 모집글 상세 `GET /api/study-posts/{postId}`

```json
// Response 200
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

- 규칙: 로그인하지 않았으면 `isAuthor`는 `false`다.
- 에러: `404 STUDY_POST_NOT_FOUND`

### 8. 모집글 수정 `PATCH /api/study-posts/{postId}`

바꿀 필드만 보낸다.

```json
// Request
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }

// Response 200
{ "postId": 10 }
```

- 에러: `400 INVALID_INPUT`, `403 NOT_POST_AUTHOR`, `404 STUDY_POST_NOT_FOUND`

### 9. 모집글 삭제 `DELETE /api/study-posts/{postId}`

```
// Response 204 (본문 없음)
```

- 규칙: 글을 삭제하면 달린 댓글도 함께 삭제된다.
- 에러: `403 NOT_POST_AUTHOR`, `404 STUDY_POST_NOT_FOUND`

## 댓글 = 스터디 신청 (10~13번)

### 10. 댓글 작성 `POST /api/study-posts/{postId}/comments`

```json
// Request
{ "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요." }

// Response 201
{ "commentId": 5 }
```

- 검증: `content` 1~500자
- 규칙: 모집이 마감된(`CLOSED`) 글에는 댓글을 쓸 수 없다.
- 에러: `400 INVALID_INPUT`, `401 UNAUTHORIZED`, `404 STUDY_POST_NOT_FOUND`, `409 STUDY_POST_CLOSED`

### 11. 댓글 목록 `GET /api/study-posts/{postId}/comments`

`GET /api/study-posts/{postId}/comments?page=0&size=20`

```json
// Response 200 (오래된 순 정렬)
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

- 에러: `404 STUDY_POST_NOT_FOUND`

### 12. 댓글 수정 `PATCH /api/comments/{commentId}`

```json
// Request
{ "content": "수정한 신청 메시지" }

// Response 200
{ "commentId": 5 }
```

- 검증: `content` 1~500자
- 에러: `400 INVALID_INPUT`, `403 NOT_COMMENT_AUTHOR`, `404 COMMENT_NOT_FOUND`

### 13. 댓글 삭제 `DELETE /api/comments/{commentId}`

```
// Response 204 (본문 없음)
```

- 에러: `403 NOT_COMMENT_AUTHOR`, `404 COMMENT_NOT_FOUND`

## 에러 코드

`ErrorCode` enum의 상수 이름과 응답 `code` 값이다. 응답 `code`는 3주차 규칙(도메인 prefix + 하이픈 + 일련번호)을 따른다.

| HTTP | ErrorCode 상수 | 응답 `code` | 설명 |
|---|---|---|---|
| 400 | `INVALID_INPUT` | `COMMON-003` | 요청 값 검증 실패 (기수가 1 미만이거나 파트가 목록에 없는 경우 포함) |
| 401 | `INVALID_CREDENTIALS` | `USER-002` | 이메일 또는 비밀번호가 틀림 |
| 401 | `UNAUTHORIZED` | `COMMON-004` | 토큰이 없거나, 유효하지 않거나, 로그아웃한 토큰 |
| 403 | `NOT_POST_AUTHOR` | `STUDY-002` | 모집글 작성자가 아님 |
| 403 | `NOT_COMMENT_AUTHOR` | `COMMENT-002` | 댓글 작성자가 아님 |
| 404 | `STUDY_POST_NOT_FOUND` | `STUDY-001` | 모집글이 없음 |
| 404 | `COMMENT_NOT_FOUND` | `COMMENT-001` | 댓글이 없음 |
| 409 | `EMAIL_ALREADY_EXISTS` | `USER-001` | 이미 가입된 이메일 |
| 409 | `STUDY_POST_CLOSED` | `STUDY-003` | 모집이 마감된 글에는 댓글을 쓸 수 없음 |

> 현재 `ErrorCode`에 `COMMON-001`, `COMMON-002`가 이미 있어서 `COMMON`의 새 코드는 003부터 시작한다.

## 데이터 모델 요약

과제가 정한 최소 구성이다. 컬럼의 이름과 의미는 바꾸지 않는다. ERD는 `docs/erd/`에 둔다.

| 테이블 | 컬럼 |
|---|---|
| User | `id`, `email`(UK), `password`, `name`, `nickname`, `generation`, `part`, `created_at`, `updated_at` |
| StudyPost | `id`, `user_id`(FK), `title`, `content`, `study_status`, `created_at`, `updated_at` |
| StudyPostComment | `id`, `study_post_id`(FK), `user_id`(FK), `content`, `created_at`, `updated_at` |

- `part`: `PM`(기획), `DE`(디자인), `BE`(백엔드), `FE`(프론트엔드)
- `study_status`: `RECRUITING`, `CLOSED`
- 연관관계는 모두 `LAZY`로 하고, Enum은 문자열로 저장한다.

## 추가 설계

과제가 허용한 범위(최소 구성에 테이블 추가)에서 우리가 정한 부분이다. 구현하며 바뀌면 ERD와 이 절을 함께 갱신한다.

### 로그아웃 토큰 무효화 테이블

JWT는 서버가 상태를 저장하지 않으므로, 로그아웃한 토큰을 서버가 따로 기억해야 한다. DB 테이블에 저장한다.

| 컬럼 | 설명 |
|---|---|
| `id` | PK |
| `jti` (UK) | 토큰 고유 ID. 토큰 전체 대신 이것만 저장한다 |
| `expires_at` | 토큰 만료 시각. 이 시각이 지나면 행은 더 이상 필요 없다 |

- 매 요청마다 JWT 필터가 `jti`가 이 테이블에 있는지 확인하고, 있으면 `401 UNAUTHORIZED`를 내려준다.
- 만료된 행의 정리 방법은 구현 시 결정한다.
- 운영 환경이라면 TTL을 지원하는 Redis로 교체하는 것이 적합하다. (이번 과제에서는 기존 MySQL만 사용한다.)
