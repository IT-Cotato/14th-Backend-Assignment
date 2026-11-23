
# 코테이토 스터디 매칭 플랫폼 API 명세서

## 1. 공통 규칙

### 1.1 기본 규칙

- 모든 API의 주소는 `/api`로 시작한다.
- 인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 사용한다.
- 권한은 공개, 로그인, 작성자로 구분한다.
    - 공개: 인증 없이 접근할 수 있다.
    - 로그인: 유효한 JWT가 필요하다.
    - 작성자: 로그인한 사용자 중 해당 리소스의 작성자만 접근할 수 있다.
- 응답은 기존 프로젝트의 공통 응답 형식으로 감싼다.
- 본 문서의 성공 응답 예시는 공통 응답의 `data` 필드에 들어가는 값이다.
- `204 No Content` 응답은 본문 없이 반환한다.
- 실패 응답은 기존 `ErrorResponse` 형식을 따른다.

### 1.2 공통 에러 응답

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

- `code`는 도메인 prefix와 일련번호를 조합한다.
- `ErrorCode` enum 상수 이름과 실제 응답의 `code` 값은 구분한다.

### 1.3 공통 데이터 규칙

- `part`: `PM`, `DE`, `BE`, `FE`
- `studyStatus`: `RECRUITING`, `CLOSED`
- 작성자 정보는 `author` 객체로 반환한다.
    - `nickname`
    - `generation`
    - `part`
- 페이지 응답은 `content`, `page`, `size`, `totalElements`, `totalPages`를 포함한다.

---

## 2. 인증 API

### 2.1 회원가입

**POST `/api/auth/signup`**

- 권한: 공개
- 설명: 이메일과 비밀번호, 사용자 정보, 기수, 파트를 입력받아 회원가입한다.
- 비밀번호는 BCrypt로 암호화하여 저장한다.

**Request Body**

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

**Response — 201 Created**

```json
{
  "userId": 1
}
```

**입력 검증**

| 필드 | 검증 조건 |
| --- | --- |
| email | 이메일 형식 |
| password | 8자 이상 |
| nickname | 2~10자 |
| generation | 1 이상의 정수 |
| part | PM, DE, BE, FE 중 하나 |

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 400 | INVALID_INPUT |
| 409 | EMAIL_ALREADY_EXISTS |

### 2.2 로그인

**POST `/api/auth/login`**

- 권한: 공개
- 설명: 이메일과 비밀번호를 검증하고 JWT 액세스 토큰을 발급한다.

**Request Body**

```json
{
  "email": "user@gmail.com",
  "password": "password1234"
}
```

**Response — 200 OK**

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer"
}
```

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 401 | INVALID_CREDENTIALS |

### 2.3 로그아웃

**POST `/api/auth/logout`**

- 권한: 로그인
- 설명: 현재 사용 중인 액세스 토큰을 무효화한다.

**Request Header**

```http
Authorization: Bearer {accessToken}
```

**Response — 204 No Content**

응답 본문 없음.

**처리 규칙**

- 서버는 로그아웃한 토큰을 만료 시각까지 무효 처리한다.
- 토큰 저장 방식은 DB, 인메모리 등 자유롭게 선택할 수 있다.
- 로그아웃한 토큰으로 다시 요청하면 401을 반환한다.
- 프론트엔드는 저장된 토큰을 삭제하고 로그인 화면으로 이동한다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 401 | UNAUTHORIZED |

### 2.4 내 정보 조회

**GET `/api/users/me`**

- 권한: 로그인
- 설명: 로그인한 사용자의 정보를 조회한다.

**Request Header**

```http
Authorization: Bearer {accessToken}
```

**Response — 200 OK**

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

**처리 규칙**

- 비밀번호는 응답에 포함하지 않는다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 401 | UNAUTHORIZED |


---

## 3. 스터디 모집글 API

### 3.1 모집글 작성

**POST `/api/study-posts`**

- 권한: 로그인
- 설명: 새로운 스터디 모집글을 작성한다.

**Request Body**

```json
{
  "title": "알고리즘 스터디 모집",
  "content": "주 2회 백준 풀이..."
}
```

**Response — 201 Created**

```json
{
  "postId": 10
}
```

**입력 검증**

| 필드 | 검증 조건 |
| --- | --- |
| title | 1~50자 |
| content | 1~2000자 |

**처리 규칙**

- 모집글 작성자는 현재 로그인한 사용자로 설정한다.
- 최초 작성 시 `studyStatus`는 `RECRUITING`으로 설정한다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 400 | INVALID_INPUT |
| 401 | UNAUTHORIZED |

### 3.2 모집글 목록 조회

**GET `/api/study-posts`**

- 권한: 공개
- 설명: 스터디 모집글을 검색하고 모집 상태 및 작성자 파트로 필터링한다.
- 정렬: 최신순

**Query Parameters**

| 파라미터 | 필수 여부 | 설명 | 기본값 |
| --- | --- | --- | --- |
| keyword | 선택 | 제목 또는 내용에 포함된 키워드 | 없음 |
| status | 선택 | RECRUITING 또는 CLOSED | 전체 |
| part | 선택 | PM, DE, BE, FE 중 작성자 파트 | 전체 |
| page | 선택 | 페이지 번호 (0부터 시작) | 0 |
| size | 선택 | 페이지 크기 | 10 |

**Request 예시**

```http
GET /api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10
```

**Response — 200 OK**

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

**처리 규칙**

- `keyword`는 모집글의 제목과 내용을 대상으로 검색한다.
- `status`는 모집 상태를 기준으로 필터링한다.
- `part`는 모집글 작성자의 파트를 기준으로 필터링한다.
- 여러 검색 및 필터 조건이 전달되면 모든 조건을 만족하는 모집글만 반환한다.
- 모집글과 사용자 정보를 연결하여 작성자의 파트를 확인한다.
- 페이지 결과는 최신순으로 정렬한다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 400 | INVALID_INPUT |

`status` 또는 `part`가 허용되지 않은 값이면 `INVALID_INPUT`을 반환한다.

### 3.3 모집글 상세 조회

**GET `/api/study-posts/{postId}`**

- 권한: 공개
- 설명: 특정 스터디 모집글의 상세 정보를 조회한다.

**Path Parameter**

| 파라미터 | 설명 |
| --- | --- |
| postId | 조회할 모집글 ID |

**Response — 200 OK**

```json
{
  "postId": 10,
  "title": "알고리즘 스터디 모집",
  "content": "주 2회 백준 풀이...",
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

**처리 규칙**

- `isAuthor`는 현재 로그인한 사용자가 모집글 작성자인지 나타낸다.
- 로그인하지 않은 경우 `isAuthor`는 `false`이다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 404 | STUDY_POST_NOT_FOUND |

### 3.4 모집글 수정

**PATCH `/api/study-posts/{postId}`**

- 권한: 작성자
- 설명: 모집글의 제목, 내용, 모집 상태를 수정한다.

**Path Parameter**

| 파라미터 | 설명 |
| --- | --- |
| postId | 수정할 모집글 ID |

**Request Body**

```json
{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "studyStatus": "CLOSED"
}
```

**Response — 200 OK**

```json
{
  "postId": 10
}
```

**처리 규칙**

- 수정하려는 필드만 요청에 포함할 수 있다.
- 모집글 작성자 본인만 수정할 수 있다.
- `studyStatus`는 `RECRUITING` 또는 `CLOSED`이다.
- 모집 상태는 두 값 사이에서 자유롭게 변경할 수 있다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 400 | INVALID_INPUT |
| 403 | NOT_POST_AUTHOR |
| 404 | STUDY_POST_NOT_FOUND |

### 3.5 모집글 삭제

**DELETE `/api/study-posts/{postId}`**

- 권한: 작성자
- 설명: 특정 스터디 모집글을 삭제한다.

**Path Parameter**

| 파라미터 | 설명 |
| --- | --- |
| postId | 삭제할 모집글 ID |

**Response — 204 No Content**

응답 본문 없음.

**처리 규칙**

- 모집글 작성자 본인만 삭제할 수 있다.
- 모집글을 삭제하면 해당 모집글에 작성된 댓글도 함께 삭제한다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 403 | NOT_POST_AUTHOR |
| 404 | STUDY_POST_NOT_FOUND |


---

## 4. 댓글(스터디 신청) API

댓글을 작성하는 것이 곧 스터디 신청이다. 별도의 스터디 가입 신청 API는 제공하지 않는다.

### 4.1 댓글 작성

**POST `/api/study-posts/{postId}/comments`**

- 권한: 로그인
- 설명: 특정 스터디 모집글에 댓글을 작성하여 스터디에 신청한다.

**Path Parameter**

| 파라미터 | 설명 |
| --- | --- |
| postId | 댓글을 작성할 모집글 ID |

**Request Body**

```json
{
  "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요."
}
```

**Response — 201 Created**

```json
{
  "commentId": 5
}
```

**입력 검증**

| 필드 | 검증 조건 |
| --- | --- |
| content | 1~500자 |

**처리 규칙**

- 댓글 작성자는 현재 로그인한 사용자로 설정한다.
- 모집 상태가 `CLOSED`인 글에는 댓글을 작성할 수 없다.
- 동일한 사용자가 하나의 모집글에 여러 댓글을 작성할 수 있다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 400 | INVALID_INPUT |
| 401 | UNAUTHORIZED |
| 404 | STUDY_POST_NOT_FOUND |
| 409 | STUDY_POST_CLOSED |

### 4.2 댓글 목록 조회

**GET `/api/study-posts/{postId}/comments`**

- 권한: 공개
- 설명: 특정 스터디 모집글에 작성된 댓글 목록을 조회한다.
- 정렬: 오래된 순

**Path Parameter**

| 파라미터 | 설명 |
| --- | --- |
| postId | 댓글 목록을 조회할 모집글 ID |

**Query Parameters**

| 파라미터 | 필수 여부 | 기본값 |
| --- | --- | --- |
| page | 선택 | 0 |
| size | 선택 | 20 |

**Request 예시**

```http
GET /api/study-posts/10/comments?page=0&size=20
```

**Response — 200 OK**

```json
{
  "content": [
    {
      "commentId": 5,
      "content": "안녕하세요! ...",
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

**처리 규칙**

- 댓글은 오래된 순으로 정렬한다.
- `isAuthor`는 현재 로그인한 사용자가 댓글 작성자인지 나타낸다.
- 로그인하지 않은 경우 `isAuthor`는 `false`이다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 404 | STUDY_POST_NOT_FOUND |

### 4.3 댓글 수정

**PATCH `/api/comments/{commentId}`**

- 권한: 작성자
- 설명: 자신이 작성한 댓글의 내용을 수정한다.

**Path Parameter**

| 파라미터 | 설명 |
| --- | --- |
| commentId | 수정할 댓글 ID |

**Request Body**

```json
{
  "content": "수정한 신청 메시지"
}
```

**Response — 200 OK**

```json
{
  "commentId": 5
}
```

**처리 규칙**

- 댓글 작성자 본인만 수정할 수 있다.
- 댓글 내용은 1~500자여야 한다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 400 | INVALID_INPUT |
| 403 | NOT_COMMENT_AUTHOR |
| 404 | COMMENT_NOT_FOUND |

### 4.4 댓글 삭제

**DELETE `/api/comments/{commentId}`**

- 권한: 작성자
- 설명: 자신이 작성한 댓글을 삭제한다.

**Path Parameter**

| 파라미터 | 설명 |
| --- | --- |
| commentId | 삭제할 댓글 ID |

**Response — 204 No Content**

응답 본문 없음.

**처리 규칙**

- 댓글 작성자 본인만 삭제할 수 있다.

**에러**

| HTTP Status | ErrorCode |
| --- | --- |
| 403 | NOT_COMMENT_AUTHOR |
| 404 | COMMENT_NOT_FOUND |

---

## 5. 에러 코드 요약

모든 실패 응답은 기존 프로젝트의 `ErrorResponse` 형식을 사용한다.

`ErrorCode`는 기존 프로젝트와 동일하게 하나의 enum에서 관리한다.

실제 응답의 `code` 값은 도메인 prefix와 일련번호를 조합하며, 아래 응답 코드 값은 과제에서 제공한 예시이다.

| HTTP Status | ErrorCode 상수 | 응답 code (예시) | 설명 |
| --- | --- | --- | --- |
| 400 | INVALID_INPUT | COMMON-003 | 요청 값 검증 실패 |
| 401 | INVALID_CREDENTIALS | USER-002 | 이메일 또는 비밀번호 불일치 |
| 401 | UNAUTHORIZED | COMMON-004 | 토큰 누락, 무효 또는 로그아웃된 토큰 |
| 403 | NOT_POST_AUTHOR | STUDY-002 | 모집글 작성자가 아님 |
| 403 | NOT_COMMENT_AUTHOR | COMMENT-002 | 댓글 작성자가 아님 |
| 404 | STUDY_POST_NOT_FOUND | STUDY-001 | 모집글이 존재하지 않음 |
| 404 | COMMENT_NOT_FOUND | COMMENT-001 | 댓글이 존재하지 않음 |
| 409 | EMAIL_ALREADY_EXISTS | USER-001 | 이미 가입된 이메일 |
| 409 | STUDY_POST_CLOSED | STUDY-003 | 모집이 마감된 글에 댓글 작성 시도 |
