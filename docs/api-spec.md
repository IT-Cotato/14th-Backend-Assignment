# API 명세

## 공통 규칙

- 모든 주소는 `/api`로 시작한다.
- 인증이 필요한 API는 요청 헤더에 `Authorization: Bearer 토큰`을 담는다.
- 권한은 세 가지다. **공개**(인증 불필요), **로그인**(토큰 필요), **작성자**(로그인 + 본인이 쓴 글·댓글).
- 성공 응답은 `ApiResponse`로 감싸고, 아래 예시는 `data` 안쪽만 적었다. 본문이 없는 `204`는 감싸지 않는다.
- 실패 응답은 모든 API에서 `ErrorResponse` 형식이다.

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

- 목록 응답은 `content`, `page`, `size`, `totalElements`, `totalPages`를 내려준다.
- 작성자 정보는 `author` 객체(`nickname`, `generation`, `part`)로 내려준다.
- `part`는 `PM`(기획), `DE`(디자인), `BE`(백엔드), `FE`(프론트엔드) 중 하나다.
- `studyStatus`는 `RECRUITING`, `CLOSED` 중 하나이고, 둘 사이를 자유롭게 바꿀 수 있다.

## 목록

| # | Method | URL | 권한 | 설명 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/api/auth/signup` | 공개 | 회원가입 |
| 2 | POST | `/api/auth/login` | 공개 | 로그인 |
| 3 | POST | `/api/auth/logout` | 로그인 | 로그아웃 |
| 4 | GET | `/api/users/me` | 로그인 | 내 정보 조회 |
| 5 | POST | `/api/study-posts` | 로그인 | 모집글 작성 |
| 6 | GET | `/api/study-posts` | 공개 | 모집글 목록 |
| 7 | GET | `/api/study-posts/{postId}` | 공개 | 모집글 상세 |
| 8 | PATCH | `/api/study-posts/{postId}` | 작성자 | 모집글 수정 |
| 9 | DELETE | `/api/study-posts/{postId}` | 작성자 | 모집글 삭제 |
| 10 | POST | `/api/study-posts/{postId}/comments` | 로그인 | 댓글(신청) 작성 |
| 11 | GET | `/api/study-posts/{postId}/comments` | 공개 | 댓글 목록 |
| 12 | PATCH | `/api/comments/{commentId}` | 작성자 | 댓글 수정 |
| 13 | DELETE | `/api/comments/{commentId}` | 작성자 | 댓글 삭제 |

## 인증

### 1. 회원가입 `POST /api/auth/signup`

요청

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

응답 `201`

```json
{ "userId": 1 }
```

- 검증: `email` 이메일 형식, `password` 8자 이상, `nickname` 2~10자, `generation` 1 이상의 정수, `part`는 PM·DE·BE·FE 중 하나
- 에러: `400 INVALID_INPUT`, `409 EMAIL_ALREADY_EXISTS`

### 2. 로그인 `POST /api/auth/login`

요청

```json
{ "email": "user@gmail.com", "password": "password1234" }
```

응답 `200`

```json
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
```

- 에러: `401 INVALID_CREDENTIALS`

### 3. 로그아웃 `POST /api/auth/logout`

응답 `204` 본문 없음

- 서버는 사용한 토큰을 만료 시각까지 무효 처리한다.
- 로그아웃한 토큰으로 다시 요청하면 `401`을 내려준다.
- 프론트는 저장해둔 토큰을 지우고 로그인 화면으로 이동한다.
- 에러: `401 UNAUTHORIZED`

### 4. 내 정보 조회 `GET /api/users/me`

응답 `200`

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

- 비밀번호는 응답에 넣지 않는다.
- 에러: `401 UNAUTHORIZED`

## 스터디 모집글

### 5. 모집글 작성 `POST /api/study-posts`

요청

```json
{ "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }
```

응답 `201`

```json
{ "postId": 10 }
```

- 검증: `title` 1~50자, `content` 1~2000자
- 처음 만들면 `studyStatus`는 `RECRUITING`이다.
- 에러: `400 INVALID_INPUT`, `401 UNAUTHORIZED`

### 6. 모집글 목록 `GET /api/study-posts`

예: `/api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10`

| 쿼리 | 필수 | 설명 |
| --- | --- | --- |
| keyword | 선택 | 제목, 내용에 포함된 키워드 |
| status | 선택 | `RECRUITING` 또는 `CLOSED`. 없으면 전체 |
| part | 선택 | `PM`, `DE`, `BE`, `FE`. 작성자의 파트가 같은 글만. 없으면 전체 |
| page | 선택 | 기본 0 |
| size | 선택 | 기본 10 |

응답 `200` (최신순)

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

- `part`는 글 작성자의 파트로 거르므로 모집글과 User를 조인한다.
- `keyword`, `status`, `part`는 함께 쓸 수 있고, 함께 쓰면 모두 만족하는 글만 나온다.
- 에러: `400 INVALID_INPUT` (status나 part 값이 올바르지 않을 때)

### 7. 모집글 상세 `GET /api/study-posts/{postId}`

응답 `200`

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

- 로그인하지 않았으면 `isAuthor`는 `false`다.
- 에러: `404 STUDY_POST_NOT_FOUND`

### 8. 모집글 수정 `PATCH /api/study-posts/{postId}`

요청 (바꿀 필드만 보낸다)

```json
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }
```

응답 `200`

```json
{ "postId": 10 }
```

- 에러: `400 INVALID_INPUT`, `403 NOT_POST_AUTHOR`, `404 STUDY_POST_NOT_FOUND`

### 9. 모집글 삭제 `DELETE /api/study-posts/{postId}`

응답 `204` 본문 없음

- 글을 삭제하면 달린 댓글도 함께 삭제된다.
- 에러: `403 NOT_POST_AUTHOR`, `404 STUDY_POST_NOT_FOUND`

## 댓글 (스터디 신청)

### 10. 댓글 작성 `POST /api/study-posts/{postId}/comments`

요청

```json
{ "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요." }
```

응답 `201`

```json
{ "commentId": 5 }
```

- 검증: `content` 1~500자
- 모집이 마감된(`CLOSED`) 글에는 댓글을 쓸 수 없다.
- 같은 사람이 댓글을 여러 번 달 수 있다.
- 에러: `400 INVALID_INPUT`, `401 UNAUTHORIZED`, `404 STUDY_POST_NOT_FOUND`, `409 STUDY_POST_CLOSED`

### 11. 댓글 목록 `GET /api/study-posts/{postId}/comments`

예: `/api/study-posts/10/comments?page=0&size=20` (`size` 기본값 20)

응답 `200` (오래된 순)

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

- 에러: `404 STUDY_POST_NOT_FOUND`

### 12. 댓글 수정 `PATCH /api/comments/{commentId}`

요청

```json
{ "content": "수정한 신청 메시지" }
```

응답 `200`

```json
{ "commentId": 5 }
```

- 에러: `400 INVALID_INPUT`, `403 NOT_COMMENT_AUTHOR`, `404 COMMENT_NOT_FOUND`

### 13. 댓글 삭제 `DELETE /api/comments/{commentId}`

응답 `204` 본문 없음

- 에러: `403 NOT_COMMENT_AUTHOR`, `404 COMMENT_NOT_FOUND`

## 에러 코드

`ErrorCode` enum의 상수 이름이다. 응답 `code`는 도메인 prefix + 하이픈 + 일련번호이고, 아래 값은 예시다.

| HTTP | 상수 이름 | 응답 code | 설명 |
| --- | --- | --- | --- |
| 400 | `INVALID_INPUT` | `COMMON-003` | 요청 값 검증 실패 |
| 401 | `INVALID_CREDENTIALS` | `USER-002` | 이메일 또는 비밀번호가 틀림 |
| 401 | `UNAUTHORIZED` | `COMMON-004` | 토큰이 없거나, 유효하지 않거나, 로그아웃한 토큰 |
| 403 | `NOT_POST_AUTHOR` | `STUDY-002` | 모집글 작성자가 아님 |
| 403 | `NOT_COMMENT_AUTHOR` | `COMMENT-002` | 댓글 작성자가 아님 |
| 404 | `STUDY_POST_NOT_FOUND` | `STUDY-001` | 모집글이 없음 |
| 404 | `COMMENT_NOT_FOUND` | `COMMENT-001` | 댓글이 없음 |
| 409 | `EMAIL_ALREADY_EXISTS` | `USER-001` | 이미 가입된 이메일 |
| 409 | `STUDY_POST_CLOSED` | `STUDY-003` | 모집이 마감된 글에는 댓글 불가 |
