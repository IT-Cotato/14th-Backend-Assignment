
# 코테이토 스터디 매칭 플랫폼 API 명세서

## 1. 프로젝트 개요

코테이토 회원을 위한 스터디 매칭 플랫폼이다.

주요 기능:
- 회원가입, 로그인, 로그아웃, 내 정보 조회
- 스터디 모집글 CRUD
- 모집글 검색, 필터링, 페이징
- 댓글 작성, 조회, 수정, 삭제

모집글에 댓글을 작성하는 것이 스터디 신청이다.

## 2. 공통 API 규칙

### Base URL

로컬 개발 환경:

`http://localhost:8080/api`

모든 API 경로는 `/api`로 시작한다.

### 인증 방식

Spring Security와 JWT를 사용한다.

인증이 필요한 요청에는 다음 헤더를 포함한다.

`Authorization: Bearer <accessToken>`

### 접근 권한

- 공개: 로그인 없이 접근 가능
- 로그인: 유효한 JWT 필요
- 작성자: 로그인 및 작성자 본인 확인 필요

### 성공 응답

기존 3주차 ApiResponse 형식을 사용한다.

```json
{
  "success": true,
  "status": 200,
  "data": {},
  "timestamp": "2026-11-08 12:00:00"
}
```

각 API의 응답 예시는 `data` 내부만 나타낸다.

HTTP 204는 응답 본문이 없다.

### 실패 응답

기존 ErrorResponse 형식을 사용한다.

```json
{
  "success": false,
  "status": 404,
  "code": "STUDY-001",
  "message": "존재하지 않는 스터디 모집글입니다.",
  "method": "GET",
  "path": "/api/study-posts/999",
  "timestamp": "2026-11-08 12:00:00"
}
```

에러 코드는 기존 ErrorCode enum에서 관리한다.

### 페이지네이션

목록 응답에는 다음 필드가 포함된다.

- content
- page
- size
- totalElements
- totalPages

페이지 번호는 0부터 시작한다.

### 작성자 정보

작성자 객체는 다음 필드를 포함한다.

- nickname
- generation
- part

## 3. API 전체 목록

| 번호 | Method | URL | 권한 | 기능 |
|---|---|---|---|---|
| 1 | POST | /api/auth/signup | 공개 | 회원가입 |
| 2 | POST | /api/auth/login | 공개 | 로그인 |
| 3 | POST | /api/auth/logout | 로그인 | 로그아웃 |
| 4 | GET | /api/users/me | 로그인 | 내 정보 조회 |
| 5 | POST | /api/study-posts | 로그인 | 모집글 작성 |
| 6 | GET | /api/study-posts | 공개 | 모집글 목록 |
| 7 | GET | /api/study-posts/{postId} | 공개 | 모집글 상세 |
| 8 | PATCH | /api/study-posts/{postId} | 작성자 | 모집글 수정 |
| 9 | DELETE | /api/study-posts/{postId} | 작성자 | 모집글 삭제 |
| 10 | POST | /api/study-posts/{postId}/comments | 로그인 | 댓글 작성 |
| 11 | GET | /api/study-posts/{postId}/comments | 공개 | 댓글 목록 |
| 12 | PATCH | /api/comments/{commentId} | 작성자 | 댓글 수정 |
| 13 | DELETE | /api/comments/{commentId} | 작성자 | 댓글 삭제 |

## 4. 인증 API

### 4-1. 회원가입

`POST /api/auth/signup`

권한: 공개

Request:

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

Response: 201 Created

```json
{
  "userId": 1
}
```

검증:
- email: 필수, 이메일 형식
- password: 필수, 8자 이상
- name: 필수
- nickname: 필수, 2~10자
- generation: 1 이상의 정수
- part: PM, DE, BE, FE 중 하나

에러:
- 400 INVALID_INPUT
- 409 EMAIL_ALREADY_EXISTS

### 4-2. 로그인

`POST /api/auth/login`

권한: 공개

Request:

```json
{
  "email": "user@gmail.com",
  "password": "password1234"
}
```

Response: 200 OK

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer"
}
```

에러:
- 401 INVALID_CREDENTIALS

### 4-3. 로그아웃

`POST /api/auth/logout`

권한: 로그인

Response: 204 No Content

규칙:
- 서버에서 JWT를 만료 시각까지 무효화한다.
- 무효화된 JWT로 요청하면 401을 반환한다.
- 프론트엔드는 토큰을 삭제하고 로그인 화면으로 이동한다.

에러:
- 401 UNAUTHORIZED

### 4-4. 내 정보 조회

`GET /api/users/me`

권한: 로그인

Response: 200 OK

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

규칙:
- 비밀번호는 응답에 포함하지 않는다.

에러:
- 401 UNAUTHORIZED

## 5. 스터디 모집글 API

### 5-1. 모집글 작성

`POST /api/study-posts`

권한: 로그인

Request:

```json
{
  "title": "알고리즘 스터디 모집",
  "content": "주 2회 백준 문제를 풉니다."
}
```

Response: 201 Created

```json
{
  "postId": 10
}
```

검증:
- title: 1~50자
- content: 1~2000자

규칙:
- 최초 모집 상태는 RECRUITING이다.
- 작성자는 로그인한 사용자이다.

에러:
- 400 INVALID_INPUT
- 401 UNAUTHORIZED

### 5-2. 모집글 목록 조회

`GET /api/study-posts`

권한: 공개

Query Parameters:

| 이름 | 필수 | 기본값 | 설명 |
|---|---|---|---|
| keyword | X | 없음 | 제목, 내용 검색 |
| status | X | 전체 | RECRUITING, CLOSED |
| part | X | 전체 | PM, DE, BE, FE |
| page | X | 0 | 페이지 번호 |
| size | X | 10 | 페이지 크기 (1~100) |

요청 예시:

`GET /api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10`

Response: 200 OK

```json
{
  "content": [
    {
      "postId": 10,
      "title": "알고리즘 스터디 모집",
      "studyStatus": "RECRUITING",
      "author": {
        "nickname": "길동이",
        "generation": 14,
        "part": "BE"
      },
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

규칙:
- 모집글 최신순 정렬
- keyword는 제목과 내용에서 검색
- part는 작성자의 파트를 기준으로 검색
- 필터를 함께 사용하면 모든 조건을 만족해야 한다.
- page가 음수이거나 size가 1~100 범위를 벗어나면 400을 반환한다.
- status, part에 정의되지 않은 값을 보내면 400을 반환한다.
- commentCount는 해당 모집글의 댓글 수이다.

에러:
- 400 INVALID_INPUT

### 5-3. 모집글 상세 조회

`GET /api/study-posts/{postId}`

권한: 공개

Response: 200 OK

```json
{
  "postId": 10,
  "title": "알고리즘 스터디 모집",
  "content": "주 2회 백준 문제를 풉니다.",
  "studyStatus": "RECRUITING",
  "author": {
    "nickname": "길동이",
    "generation": 14,
    "part": "BE"
  },
  "isAuthor": true,
  "createdAt": "2026-11-08T12:00:00",
  "updatedAt": "2026-11-08T12:00:00"
}
```

규칙:
- isAuthor는 현재 사용자가 작성자인지 나타낸다.
- 비로그인 사용자는 isAuthor가 false이다.

에러:
- 404 STUDY_POST_NOT_FOUND

### 5-4. 모집글 수정

`PATCH /api/study-posts/{postId}`

권한: 작성자

Request:

```json
{
  "title": "수정된 제목",
  "content": "수정된 모집 내용",
  "studyStatus": "CLOSED"
}
```

Response: 200 OK

```json
{
  "postId": 10
}
```

규칙:
- 작성자 본인만 수정 가능
- 변경할 필드만 전달 가능
- 모집 상태 변경 가능
- 상태는 RECRUITING, CLOSED 중 하나
- title은 1~50자, content는 1~2000자이며, 전달한 경우 공백만으로 채울 수 없다.

에러:
- 400 INVALID_INPUT
- 401 UNAUTHORIZED
- 403 NOT_POST_AUTHOR
- 404 STUDY_POST_NOT_FOUND

### 5-5. 모집글 삭제

`DELETE /api/study-posts/{postId}`

권한: 작성자

Response: 204 No Content

규칙:
- 작성자 본인만 삭제 가능
- 모집글 삭제 시 댓글도 함께 삭제

에러:
- 401 UNAUTHORIZED
- 403 NOT_POST_AUTHOR
- 404 STUDY_POST_NOT_FOUND

## 6. 댓글 API

### 6-1. 댓글 작성

`POST /api/study-posts/{postId}/comments`

권한: 로그인

Request:

```json
{
  "content": "안녕하세요! 스터디에 참여하고 싶어요."
}
```

Response: 201 Created

```json
{
  "commentId": 5
}
```

검증:
- content: 1~500자

규칙:
- 댓글 작성은 스터디 신청을 의미한다.
- CLOSED 상태에서는 댓글을 작성할 수 없다.
- 동일 사용자가 여러 댓글을 작성할 수 있다.

에러:
- 400 INVALID_INPUT
- 401 UNAUTHORIZED
- 404 STUDY_POST_NOT_FOUND
- 409 STUDY_POST_CLOSED

### 6-2. 댓글 목록 조회

`GET /api/study-posts/{postId}/comments`

권한: 공개

Query Parameters:
- page: 기본값 0 (0 이상)
- size: 기본값 20 (1~100)

Response: 200 OK

```json
{
  "content": [
    {
      "commentId": 5,
      "content": "스터디에 참여하고 싶어요.",
      "author": {
        "nickname": "철수",
        "generation": 13,
        "part": "FE"
      },
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

규칙:
- 오래된 댓글부터 정렬
- 작성자 정보 포함
- 현재 사용자 기준 isAuthor 반환

에러:
- 400 INVALID_INPUT
- 404 STUDY_POST_NOT_FOUND

### 6-3. 댓글 수정

`PATCH /api/comments/{commentId}`

권한: 작성자

Request:

```json
{
  "content": "수정된 스터디 신청 메시지"
}
```

Response: 200 OK

```json
{
  "commentId": 5
}
```

검증:
- content: 1~500자

규칙:
- 작성자 본인만 수정 가능
- 모집글 작성자도 다른 사람의 댓글은 수정·삭제할 수 없다.

에러:
- 400 INVALID_INPUT
- 401 UNAUTHORIZED
- 403 NOT_COMMENT_AUTHOR
- 404 COMMENT_NOT_FOUND

### 6-4. 댓글 삭제

`DELETE /api/comments/{commentId}`

권한: 작성자

Response: 204 No Content

규칙:
- 작성자 본인만 삭제 가능

에러:
- 401 UNAUTHORIZED
- 403 NOT_COMMENT_AUTHOR
- 404 COMMENT_NOT_FOUND

## 7. 에러 코드

| HTTP | ErrorCode | 응답 코드 | 의미 |
|---|---|---|---|
| 400 | INVALID_INPUT | COMMON-003 | 입력값 검증 실패 |
| 401 | INVALID_CREDENTIALS | USER-002 | 이메일 또는 비밀번호 오류 |
| 401 | UNAUTHORIZED | COMMON-004 | 인증 실패 |
| 403 | NOT_POST_AUTHOR | STUDY-002 | 모집글 작성자가 아님 |
| 403 | NOT_COMMENT_AUTHOR | COMMENT-002 | 댓글 작성자가 아님 |
| 404 | STUDY_POST_NOT_FOUND | STUDY-001 | 모집글이 존재하지 않음 |
| 404 | COMMENT_NOT_FOUND | COMMENT-001 | 댓글이 존재하지 않음 |
| 404 | COMMON_NOT_FOUND | COMMON-005 | 존재하지 않는 API URL |
| 405 | METHOD_NOT_ALLOWED | COMMON-006 | 지원하지 않는 HTTP 메서드 (Allow 헤더 포함) |
| 409 | EMAIL_ALREADY_EXISTS | USER-001 | 이메일 중복 |
| 409 | STUDY_POST_CLOSED | STUDY-003 | 모집 마감으로 댓글 작성 불가 |
| 500 | COMMON_INTERNAL_SERVER_ERROR | COMMON-002 | 처리하지 못한 서버 내부 오류 |

404(COMMON-005)와 405(COMMON-006)는 Security 인증을 통과한 뒤에 판단한다.
로그인이 필요한 경로를 토큰 없이 요청하면 먼저 401(COMMON-004)을 반환한다.

응답 코드는 `backend/.../global/exception/ErrorCode.java`의 실제 값과 같다.
`COMMON-001`(BAD_REQUEST)은 정의만 되어 있고 사용하지 않으며, `BOOK-001`은 1~3주차 레거시 API용이다.

## 8. 데이터 모델

### User
- id: PK
- email: UNIQUE
- password
- name
- nickname
- generation
- part
- created_at
- updated_at

### StudyPost
- id: PK
- user_id: FK → User.id
- title
- content
- study_status
- created_at
- updated_at

### StudyPostComment
- id: PK
- study_post_id: FK → StudyPost.id
- user_id: FK → User.id
- content
- created_at
- updated_at

### RevokedToken
- id: PK
- token_hash: UNIQUE, 로그아웃한 JWT의 SHA-256 hex(64자)
- expires_at: 원래 JWT의 만료 시각
- created_at
- updated_at

JWT 원문은 저장하지 않는다.
JWT 검증 시 토큰 해시가 이 테이블에 있으면 401을 반환한다.
다른 테이블과 연관관계가 없는 독립 테이블이다.

### 테이블 관계

- User 1:N StudyPost
- User 1:N StudyPostComment
- StudyPost 1:N StudyPostComment

모든 관계는 비식별 관계이다.

StudyPost를 삭제하면 해당 StudyPostComment도 함께 삭제된다.
(서비스에서 댓글을 먼저 일괄 삭제하고, study_post_id FK에도 ON DELETE CASCADE가 걸려 있다.)

JPA 연관관계는 LAZY 로딩을 사용한다.
Enum은 문자열로 저장한다.

## 9. ERD

ERDCloud에서 설계한 ERD를 사용한다.

ERD 이미지:
`docs/erd/erdCloud.png`

ERDCloud 링크:
https://www.erdcloud.com/p/g93fkp8XtYENgYqpz

## 10. 참고 사항

- 3주차 공통 응답 및 예외 처리 구조를 유지한다.
- JWT는 accessToken 하나를 사용한다.
- 로그아웃한 JWT는 만료 시각까지 무효화한다.
- 모든 API는 Swagger로 문서화한다.
- 프론트엔드는 Axios를 통해 API를 호출한다.
- Playwright로 주요 정상 및 에러 시나리오를 검증한다.
