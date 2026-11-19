# API 명세

- Base URL: `http://localhost:8080`
- 모든 API는 `/api`로 시작한다.
- 인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 보낸다.
- 권한: **공개**(인증 불필요), **로그인**(토큰 필요), **작성자**(로그인 + 작성자 본인)
- Swagger: `http://localhost:8080/swagger-ui/index.html`

## 공통 응답

성공 응답은 `ApiResponse`로 감싼다. 아래 각 API의 응답 예시는 `data` 안쪽만 적었다.

```json
{ "success": true, "status": 200, "data": { }, "timestamp": "2026-11-08 12:00:00" }
```

본문이 없는 `204`는 감싸지 않는다.

실패 응답은 모든 API에서 `ErrorResponse` 형식이다.

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

입력 검증 실패(`COMMON-003`)의 `message`는 어떤 값이 잘못됐는지 알려주는 문장이다. (예: `기수는 1 이상의 정수로 입력해 주세요.`)

목록 응답은 `content`, `page`, `size`, `totalElements`, `totalPages`를 내려준다.
작성자 정보는 `author` 객체(`nickname`, `generation`, `part`)로 내려준다.
날짜(`createdAt`, `updatedAt`)는 `yyyy-MM-ddTHH:mm:ss` 형식이다.

## 목록

| # | Method | URL | 권한 | 설명 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/api/auth/signup` | 공개 | 회원가입 (기수, 파트 포함) |
| 2 | POST | `/api/auth/login` | 공개 | 로그인 (토큰 발급) |
| 3 | POST | `/api/auth/logout` | 로그인 | 로그아웃 (토큰 무효화) |
| 4 | GET | `/api/users/me` | 로그인 | 내 정보 조회 |
| 5 | POST | `/api/study-posts` | 로그인 | 모집글 작성 |
| 6 | GET | `/api/study-posts` | 공개 | 모집글 목록 (검색, 모집 상태와 파트 필터, 페이징) |
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

[검증] email 필수, 형식, 100자 이하 / password 8~64자 / name 1~20자 / nickname 2~10자
       generation 1 이상의 정수 / part는 PM, DE, BE, FE 중 하나
[규칙] 비밀번호는 BCrypt로 저장한다. 이메일은 앞뒤 공백을 지우고 소문자로 저장한다.
[에러] 400 INVALID_INPUT, 409 EMAIL_ALREADY_EXISTS
```

### 2. 로그인 `POST /api/auth/login`

```
[Request]
{ "email": "user@gmail.com", "password": "password1234" }

[Response 200]
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }

[규칙] 토큰 유효 시간은 기본 1시간(JWT_ACCESS_TOKEN_EXPIRATION). 리프레시 토큰은 없다.
[에러] 400 INVALID_INPUT, 401 INVALID_CREDENTIALS
```

### 3. 로그아웃 `POST /api/auth/logout`

```
[Response 204] 본문 없음

[규칙] 요청에 쓴 토큰의 SHA-256 해시를 blacklisted_token 테이블에 만료 시각과 함께 저장한다.
       같은 토큰으로 다시 요청하면 401이다. 만료된 기록은 한 시간마다 지운다.
       프론트는 저장해둔 토큰을 지우고 로그인 화면으로 이동한다.
[에러] 401 UNAUTHORIZED
```

### 4. 내 정보 조회 `GET /api/users/me`

```
[Response 200]
{ "userId": 1, "email": "user@gmail.com", "name": "홍길동", "nickname": "길동이",
  "generation": 14, "part": "BE" }

[규칙] 비밀번호는 응답에 넣지 않는다.
[에러] 401 UNAUTHORIZED
```

## 스터디 모집글

### 5. 모집글 작성 `POST /api/study-posts`

```
[Request]
{ "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }

[Response 201]
{ "postId": 10 }

[검증] title 1~50자 / content 1~2000자 (공백만 있으면 안 됨)
[규칙] 처음 만들면 studyStatus는 RECRUITING이다.
[에러] 400 INVALID_INPUT, 401 UNAUTHORIZED
```

### 6. 모집글 목록 `GET /api/study-posts`

```
[Query]
keyword  (선택) 제목 또는 내용에 포함된 키워드. %, _ 도 글자 그대로 찾는다
status   (선택) RECRUITING 또는 CLOSED. 없으면 전체
part     (선택) PM, DE, BE, FE. 작성자의 파트가 같은 글만. 없으면 전체
page     (선택) 기본 0, 0 이상
size     (선택) 기본 10, 1~100

[Response 200] 최신순 정렬
{
  "content": [
    { "postId": 10, "title": "알고리즘 스터디 모집", "studyStatus": "RECRUITING",
      "author": { "nickname": "길동이", "generation": 14, "part": "BE" },
      "commentCount": 3, "createdAt": "2026-11-08T12:00:00" }
  ],
  "page": 0, "size": 10, "totalElements": 1, "totalPages": 1
}

[규칙] 모집글과 User를 조인해 작성자 파트로 거른다. 세 조건은 함께 쓸 수 있고, 모두 만족하는 글만 나온다.
       댓글 수는 페이지의 글 id로 한 번에 group by 해서 센다.
[에러] 400 INVALID_INPUT (status, part, page, size 값이 올바르지 않을 때)
```

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

[규칙] 로그인하지 않았거나 토큰이 무효면 isAuthor는 false다.
[에러] 404 STUDY_POST_NOT_FOUND
```

### 8. 모집글 수정 `PATCH /api/study-posts/{postId}`

```
[Request] 바꿀 필드만 보낸다
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }

[Response 200]
{ "postId": 10 }

[검증] 보낸 필드만 검사한다. title 1~50자, content 1~2000자, studyStatus는 RECRUITING 또는 CLOSED
[규칙] 모집 상태는 RECRUITING과 CLOSED 사이를 자유롭게 바꿀 수 있다.
[에러] 400 INVALID_INPUT, 401 UNAUTHORIZED, 403 NOT_POST_AUTHOR, 404 STUDY_POST_NOT_FOUND
```

### 9. 모집글 삭제 `DELETE /api/study-posts/{postId}`

```
[Response 204] 본문 없음

[규칙] 달린 댓글을 먼저 삭제한 뒤 글을 삭제한다. (한 트랜잭션)
[에러] 401 UNAUTHORIZED, 403 NOT_POST_AUTHOR, 404 STUDY_POST_NOT_FOUND
```

## 댓글 = 스터디 신청

### 10. 댓글 작성 `POST /api/study-posts/{postId}/comments`

```
[Request]
{ "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요." }

[Response 201]
{ "commentId": 5 }

[검증] content 1~500자 (공백만 있으면 안 됨)
[규칙] 모집이 마감된(CLOSED) 글에는 쓸 수 없다. 같은 사람이 여러 번 써도 된다.
[에러] 400 INVALID_INPUT, 401 UNAUTHORIZED, 404 STUDY_POST_NOT_FOUND, 409 STUDY_POST_CLOSED
```

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

[Query] page 기본 0, size 기본 20 (1~100)
[에러] 400 INVALID_INPUT, 404 STUDY_POST_NOT_FOUND
```

### 12. 댓글 수정 `PATCH /api/comments/{commentId}`

```
[Request]
{ "content": "수정한 신청 메시지" }

[Response 200]
{ "commentId": 5 }

[에러] 400 INVALID_INPUT, 401 UNAUTHORIZED, 403 NOT_COMMENT_AUTHOR, 404 COMMENT_NOT_FOUND
```

### 13. 댓글 삭제 `DELETE /api/comments/{commentId}`

```
[Response 204] 본문 없음

[에러] 401 UNAUTHORIZED, 403 NOT_COMMENT_AUTHOR, 404 COMMENT_NOT_FOUND
```

## 에러 코드

| HTTP | ErrorCode | code | message |
| --- | --- | --- | --- |
| 400 | `INVALID_INPUT` | `COMMON-003` | 입력값이 올바르지 않아요. (검증 실패 시 필드별 문장) |
| 401 | `UNAUTHORIZED` | `COMMON-004` | 로그인이 필요해요. |
| 404 | `RESOURCE_NOT_FOUND` | `COMMON-005` | 요청한 주소를 찾을 수 없어요. |
| 405 | `METHOD_NOT_ALLOWED` | `COMMON-006` | 지원하지 않는 HTTP 메서드예요. |
| 409 | `EMAIL_ALREADY_EXISTS` | `USER-001` | 이미 가입된 이메일이에요. |
| 401 | `INVALID_CREDENTIALS` | `USER-002` | 이메일 또는 비밀번호가 올바르지 않아요. |
| 404 | `STUDY_POST_NOT_FOUND` | `STUDY-001` | 존재하지 않는 스터디 모집글이에요. |
| 403 | `NOT_POST_AUTHOR` | `STUDY-002` | 모집글 작성자만 수정하거나 삭제할 수 있어요. |
| 409 | `STUDY_POST_CLOSED` | `STUDY-003` | 모집이 마감된 글에는 댓글을 쓸 수 없어요. |
| 404 | `COMMENT_NOT_FOUND` | `COMMENT-001` | 존재하지 않는 댓글이에요. |
| 403 | `NOT_COMMENT_AUTHOR` | `COMMENT-002` | 댓글 작성자만 수정하거나 삭제할 수 있어요. |

`COMMON-001`(잘못된 요청), `COMMON-002`(서버 오류), `BOOK-001`은 3주차부터 있던 코드다.
`RESOURCE_NOT_FOUND`, `METHOD_NOT_ALLOWED`는 없는 주소나 메서드가 500으로 나가지 않도록 추가했다.

## 명세 외 추가 사항

| 구분 | 내용 |
| --- | --- |
| 테이블 | `blacklisted_token(id, token_hash UK, expires_at)`: 로그아웃한 토큰 |
| 에러 코드 | `COMMON-005`, `COMMON-006` |
| 응답 필드 | 추가 없음 |
