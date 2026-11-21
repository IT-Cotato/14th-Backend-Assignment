# API 명세

코테이토 스터디 매칭 플랫폼 API. 과제 명세(13개)를 그대로 구현했고, 추가하거나 달라진 점은 맨 아래 "명세와 다른 점"에 적는다.

## 공통 규칙

- 모든 API 주소는 `/api` 로 시작한다.
- 인증이 필요한 API 는 `Authorization: Bearer <accessToken>` 헤더를 보낸다.
- 권한: **공개**(인증 불필요), **로그인**(토큰 필요), **작성자**(로그인 + 본인이 쓴 글/댓글)
- 성공 응답은 공통 응답으로 감싼다. 아래 응답 예시는 `data` 안쪽만 적는다.

```json
{ "success": true, "status": 200, "data": { }, "timestamp": "2026-11-19 14:00:00" }
```

- 본문이 없는 `204` 는 감싸지 않는다.
- 실패 응답

```json
{
  "success": false,
  "status": 404,
  "code": "STUDY-001",
  "message": "존재하지 않는 스터디 모집글입니다.",
  "method": "GET",
  "path": "/api/study-posts/999",
  "timestamp": "2026-11-19 14:00:00"
}
```

- 페이지 응답: `content`, `page`, `size`, `totalElements`, `totalPages`
- 작성자 정보: `author` 객체 (`nickname`, `generation`, `part`)
- `part`: `PM`(기획), `DE`(디자인), `BE`(백엔드), `FE`(프론트엔드)
- `studyStatus`: `RECRUITING`(모집 중), `CLOSED`(마감)
- `createdAt`, `updatedAt` 형식: `yyyy-MM-dd'T'HH:mm:ss`

## 목록

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

## 인증

### 1. 회원가입 `POST /api/auth/signup`

```
[Request]
{ "email": "user@gmail.com", "password": "password1234", "name": "홍길동",
  "nickname": "길동이", "generation": 14, "part": "BE" }

[Response 201]
{ "userId": 1 }
```

| 필드 | 검증 | 실패 메시지 |
| --- | --- | --- |
| `email` | 이메일 형식 | 이메일 형식이 올바르지 않습니다. |
| `password` | 8자 이상 | 비밀번호는 8자 이상이어야 합니다. |
| `name` | 비어 있지 않음 | 이름을 입력해야 합니다. |
| `nickname` | 2~10자 | 닉네임은 2자 이상 10자 이하여야 합니다. |
| `generation` | 1 이상의 정수 | 기수는 1 이상의 정수여야 합니다. |
| `part` | PM, DE, BE, FE 중 하나 | 파트는 PM, DE, BE, FE 중 하나여야 합니다. (목록에 없는 값이면 "입력값이 올바르지 않습니다.") |

- 비밀번호는 BCrypt 로 암호화해서 저장한다.
- 에러: 400 `INVALID_INPUT`, 409 `EMAIL_ALREADY_EXISTS`

### 2. 로그인 `POST /api/auth/login`

```
[Request]
{ "email": "user@gmail.com", "password": "password1234" }

[Response 200]
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
```

- 이메일이 없는 경우와 비밀번호가 틀린 경우를 구분하지 않는다.
- 에러: 400 `INVALID_INPUT` (이메일이나 비밀번호가 비어 있을 때), 401 `INVALID_CREDENTIALS`

### 3. 로그아웃 `POST /api/auth/logout`

```
[Response 204] 본문 없음
```

- 사용한 토큰을 만료 시각과 함께 `logout_token` 테이블에 저장한다. 같은 토큰으로 다시 요청하면 401 이다.
- 에러: 401 `UNAUTHORIZED`

### 4. 내 정보 조회 `GET /api/users/me`

```
[Response 200]
{ "userId": 1, "email": "user@gmail.com", "name": "홍길동", "nickname": "길동이",
  "generation": 14, "part": "BE" }
```

- 비밀번호는 응답에 넣지 않는다.
- 에러: 401 `UNAUTHORIZED`

## 스터디 모집글

### 5. 모집글 작성 `POST /api/study-posts`

```
[Request]
{ "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }

[Response 201]
{ "postId": 10 }
```

- 검증: `title` 1~50자, `content` 1~2000자 (공백만 있는 값은 불가)
- 처음 만들면 `studyStatus` 는 `RECRUITING` 이다.
- 에러: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`

### 6. 모집글 목록 `GET /api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10`

| Query | 필수 | 설명 |
| --- | --- | --- |
| `keyword` | 선택 | 제목 또는 내용에 포함된 키워드 |
| `status` | 선택 | `RECRUITING` 또는 `CLOSED`. 없으면 전체 |
| `part` | 선택 | 작성자의 파트. 없으면 전체 |
| `page` | 선택 | 기본 0 |
| `size` | 선택 | 기본 10, 최대 100 |

```
[Response 200] 최신순 정렬
{
  "content": [
    { "postId": 10, "title": "알고리즘 스터디 모집", "studyStatus": "RECRUITING",
      "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
      "commentCount": 3, "createdAt": "2026-11-08T12:00:00" }
  ],
  "page": 0, "size": 10, "totalElements": 1, "totalPages": 1
}
```

- `keyword`, `status`, `part` 를 함께 쓰면 모두 만족하는 글만 나온다.
- 에러: 400 `INVALID_INPUT` (`status`, `part` 값이 올바르지 않거나 `page`, `size` 가 숫자가 아닐 때)

### 7. 모집글 상세 `GET /api/study-posts/{postId}`

```
[Response 200]
{
  "postId": 10, "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이...",
  "studyStatus": "RECRUITING",
  "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
  "isAuthor": true,
  "createdAt": "2026-11-08T12:00:00", "updatedAt": "2026-11-08T12:00:00"
}
```

- 로그인하지 않았거나 토큰이 유효하지 않으면 `isAuthor` 는 `false` 다.
- 에러: 404 `STUDY_POST_NOT_FOUND`

### 8. 모집글 수정 `PATCH /api/study-posts/{postId}`

```
[Request] 바꿀 필드만 보낸다
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }

[Response 200]
{ "postId": 10 }
```

- 보내지 않은 필드는 그대로 둔다. 보낸 필드는 작성과 같은 길이 규칙으로 검증한다.
- `RECRUITING` 과 `CLOSED` 사이를 자유롭게 바꿀 수 있다.
- 에러: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 403 `NOT_POST_AUTHOR`, 404 `STUDY_POST_NOT_FOUND`

### 9. 모집글 삭제 `DELETE /api/study-posts/{postId}`

```
[Response 204] 본문 없음
```

- 글을 삭제하면 달린 댓글도 함께 삭제된다.
- 에러: 401 `UNAUTHORIZED`, 403 `NOT_POST_AUTHOR`, 404 `STUDY_POST_NOT_FOUND`

## 댓글 (스터디 신청)

### 10. 댓글 작성 `POST /api/study-posts/{postId}/comments`

```
[Request]
{ "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요." }

[Response 201]
{ "commentId": 5 }
```

- 검증: `content` 1~500자 (공백만 있는 값은 불가)
- 모집이 마감된(`CLOSED`) 글에는 댓글을 쓸 수 없다. 같은 사람이 여러 번 써도 된다.
- 에러: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 404 `STUDY_POST_NOT_FOUND`, 409 `STUDY_POST_CLOSED`

### 11. 댓글 목록 `GET /api/study-posts/{postId}/comments?page=0&size=20`

```
[Response 200] 오래된 순 정렬
{
  "content": [
    { "commentId": 5, "content": "안녕하세요! ...",
      "author": { "nickname": "철수", "generation": 13, "part": "FE" },
      "isAuthor": false, "createdAt": "2026-11-08T13:00:00" }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

- `size` 기본 20, 최대 100
- 에러: 404 `STUDY_POST_NOT_FOUND`

### 12. 댓글 수정 `PATCH /api/comments/{commentId}`

```
[Request]
{ "content": "수정한 신청 메시지" }

[Response 200]
{ "commentId": 5 }
```

- 에러: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 403 `NOT_COMMENT_AUTHOR`, 404 `COMMENT_NOT_FOUND`

### 13. 댓글 삭제 `DELETE /api/comments/{commentId}`

```
[Response 204] 본문 없음
```

- 에러: 401 `UNAUTHORIZED`, 403 `NOT_COMMENT_AUTHOR`, 404 `COMMENT_NOT_FOUND`

## 에러 코드

| HTTP | ErrorCode 상수 | 응답 `code` | 메시지 |
| --- | --- | --- | --- |
| 400 | `INVALID_INPUT` | `COMMON-003` | 입력값이 올바르지 않습니다. (본문 검증 실패는 필드별 메시지) |
| 401 | `UNAUTHORIZED` | `COMMON-004` | 인증이 필요합니다. |
| 404 | `RESOURCE_NOT_FOUND` | `COMMON-005` | 요청한 주소를 찾을 수 없습니다. |
| 405 | `METHOD_NOT_ALLOWED` | `COMMON-006` | 지원하지 않는 HTTP 메서드입니다. (`Allow` 헤더에 쓸 수 있는 메서드) |
| 409 | `EMAIL_ALREADY_EXISTS` | `USER-001` | 이미 가입된 이메일입니다. |
| 401 | `INVALID_CREDENTIALS` | `USER-002` | 이메일 또는 비밀번호가 올바르지 않습니다. |
| 404 | `STUDY_POST_NOT_FOUND` | `STUDY-001` | 존재하지 않는 스터디 모집글입니다. |
| 403 | `NOT_POST_AUTHOR` | `STUDY-002` | 모집글 작성자만 수정하거나 삭제할 수 있습니다. |
| 409 | `STUDY_POST_CLOSED` | `STUDY-003` | 모집이 마감된 글에는 댓글을 작성할 수 없습니다. |
| 404 | `COMMENT_NOT_FOUND` | `COMMENT-001` | 존재하지 않는 댓글입니다. |
| 403 | `NOT_COMMENT_AUTHOR` | `COMMENT-002` | 댓글 작성자만 수정하거나 삭제할 수 있습니다. |

## 명세와 다른 점 (추가한 것)

- **추가 테이블 `logout_token`**: 로그아웃한 토큰을 만료 시각까지 무효 처리하려고 추가했다. (`docs/erd/README.md`)
- **테이블 이름 `users`**: `user` 는 MySQL 예약어라서 `users` 로 만들었다. 컬럼 이름과 의미는 명세와 같다.
- **검증 실패 메시지**: `INVALID_INPUT` 의 `code` 와 `status` 는 그대로이고, 본문 검증 실패일 때 `message` 만 필드별 메시지로 내려준다.
- **로그인 400**: 이메일이나 비밀번호가 비어 있으면 401 대신 400 `INVALID_INPUT` 이다.
- **페이지 크기 상한**: `size` 는 최대 100 으로 제한하고, 음수 `page` 는 0 으로 처리한다.
- **공통 404, 405**: 없는 주소는 404 `RESOURCE_NOT_FOUND`, 지원하지 않는 HTTP 메서드는 405 `METHOD_NOT_ALLOWED` 다. 단, 인증이 필요한 `/api/**` 경로는 Security 가 먼저 검사하므로 토큰이 없으면 401 이 먼저 나온다.
- 요청/응답에 명세에 없는 필드는 추가하지 않았다.
