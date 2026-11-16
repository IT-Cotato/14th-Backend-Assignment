# API 명세 (4주차 스터디 모집 게시판)

- 출처: 과제 원문 4-3(공통 규칙), 4-4(API 명세), `docs/erd/`
- 과제에서 정하지 않았거나 "예시"로만 준 부분은 논의해서 정했고, 결과는 [5. 결정 사항](#5-결정-사항)에 모았다. 과제 원문에 없는 에러는 표에 "(과제 목록에 없지만 추가)"로 적었다.

## 1. 공통 규칙

### 기본
- 모든 API 주소는 `/api`로 시작한다.
- 인증이 필요한 API는 헤더에 `Authorization: Bearer {accessToken}`을 담는다. 토큰은 `accessToken` 하나만 쓴다. (refresh token 없음)
- 명세에 없는 세부 규칙은 **쉬운 쪽으로** 구현한다. (예: 모집 상태는 RECRUITING ↔ CLOSED 자유롭게 변경, 같은 사람이 댓글 여러 번 작성 가능)

### 권한
| 권한 | 의미 |
| --- | --- |
| 공개 | 토큰 없이 호출 가능 |
| 로그인 | 유효한 토큰 필요 |
| 작성자 | 로그인 + 대상 글/댓글의 작성자 본인 |

### 성공 응답
`ApiResponse<T>`로 감싼다. **아래 API별 응답 예시는 `data` 안쪽만 적는다.**

```json
{
  "success": true,
  "status": 200,
  "data": { "postId": 10 },
  "timestamp": "2026-11-08 12:00:00"
}
```

- 본문이 없는 `204` 응답은 감싸지 않고 본문 없이 내려준다. (로그아웃, 모집글 삭제, 댓글 삭제)

### 실패 응답
모든 API에서 `ErrorResponse` 형식을 쓴다.

```json
{
  "success": false,
  "status": 404,
  "code": "STUDY-001",
  "message": "스터디 모집글을 찾을 수 없습니다.",
  "method": "GET",
  "path": "/api/study-posts/999",
  "timestamp": "2026-11-08 12:00:00"
}
```

- `code`는 `도메인 prefix-세 자리 번호` 형식이다.
- 이 문서의 `STUDY_POST_NOT_FOUND` 같은 이름은 `ErrorCode` enum **상수 이름**이다. 실제 `code`/`message` 값은 [3. 에러 코드 표](#3-에러-코드-표)를 따른다.

### 페이지 응답
목록 API는 다음 형식으로 내려준다.

```json
{ "content": [ ... ], "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
```

- `page`는 0 이상, `size`는 1~100이다. 벗어나면 400 `INVALID_INPUT`을 내려준다. (보정하지 않음)

### 작성자 확인 순서
작성자 전용 API(8, 9, 12, 13)는 다음 순서로 확인한다.
1. 토큰 확인 → 없거나 유효하지 않으면 401 `UNAUTHORIZED`
2. 대상 조회 → 없으면 404 (`STUDY_POST_NOT_FOUND` / `COMMENT_NOT_FOUND`)
3. 작성자 확인 → 본인이 아니면 403 (`NOT_POST_AUTHOR` / `NOT_COMMENT_AUTHOR`)

### 공통 객체 / Enum
- `author` 객체: `{ "nickname": "길동이", "generation": 14, "part": "BE" }`
- `part`: `PM`, `DE`, `BE`, `FE`
- `studyStatus`: `RECRUITING`, `CLOSED`

### 날짜 형식
- `timestamp`(공통 응답): `yyyy-MM-dd HH:mm:ss` (기존 `ApiResponse`, `ErrorResponse`에 이미 적용됨)
- `createdAt`, `updatedAt`: ISO-8601 `yyyy-MM-dd'T'HH:mm:ss` (예: `2026-11-08T12:00:00`). 과제 예시를 따르고, `timestamp`와는 형식이 다르다.

---

## 2. API 목록

| # | Method | URL | 권한 | 설명 | 성공 |
| --- | --- | --- | --- | --- | --- |
| 1 | POST | `/api/auth/signup` | 공개 | 회원가입 | 201 |
| 2 | POST | `/api/auth/login` | 공개 | 로그인 (토큰 발급) | 200 |
| 3 | POST | `/api/auth/logout` | 로그인 | 로그아웃 (토큰 무효화) | 204 |
| 4 | GET | `/api/users/me` | 로그인 | 내 정보 조회 | 200 |
| 5 | POST | `/api/study-posts` | 로그인 | 모집글 작성 | 201 |
| 6 | GET | `/api/study-posts` | 공개 | 모집글 목록 (검색, 필터, 페이징) | 200 |
| 7 | GET | `/api/study-posts/{postId}` | 공개 | 모집글 상세 | 200 |
| 8 | PATCH | `/api/study-posts/{postId}` | 작성자 | 모집글 수정 (모집 상태 포함) | 200 |
| 9 | DELETE | `/api/study-posts/{postId}` | 작성자 | 모집글 삭제 (댓글 함께 삭제) | 204 |
| 10 | POST | `/api/study-posts/{postId}/comments` | 로그인 | 댓글(신청) 작성 | 201 |
| 11 | GET | `/api/study-posts/{postId}/comments` | 공개 | 댓글 목록 | 200 |
| 12 | PATCH | `/api/comments/{commentId}` | 작성자 | 댓글 수정 | 200 |
| 13 | DELETE | `/api/comments/{commentId}` | 작성자 | 댓글 삭제 | 204 |

---

### 인증 (1~4)

#### 1. 회원가입
`POST /api/auth/signup` · 권한: 공개

**Request Body**
| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| email | String | O | 이메일 형식 |
| password | String | O | 8자 이상 |
| name | String | O | 비어 있으면 안 됨 (길이 제한 없음) |
| nickname | String | O | 2~10자 |
| generation | Integer | O | 1 이상의 정수 |
| part | String(enum) | O | `PM`, `DE`, `BE`, `FE` 중 하나 |

```json
{ "email": "user@gmail.com", "password": "password1234", "name": "홍길동",
  "nickname": "길동이", "generation": 14, "part": "BE" }
```

**Response `201`**
```json
{ "userId": 1 }
```

**규칙**
- 비밀번호는 암호화해서 저장한다.
- email은 중복될 수 없다. (ERD: `email` UNIQUE)
- nickname은 중복을 허용한다. password는 최대 길이 제한이 없다.

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 검증 실패 (기수 1 미만, 파트가 목록에 없음 포함) |
| 409 | `EMAIL_ALREADY_EXISTS` | 이미 가입된 이메일 |

#### 2. 로그인
`POST /api/auth/login` · 권한: 공개

**Request Body**
| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| email | String | O | 비어 있으면 안 됨 |
| password | String | O | 비어 있으면 안 됨 |

```json
{ "email": "user@gmail.com", "password": "password1234" }
```

**Response `200`**
```json
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
```

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | email 또는 password가 비어 있음 (과제 목록에 없지만 추가) |
| 401 | `INVALID_CREDENTIALS` | 가입되지 않은 이메일이거나 비밀번호가 틀림 |

#### 3. 로그아웃
`POST /api/auth/logout` · 권한: 로그인

**Request**: 헤더 `Authorization: Bearer {accessToken}`, 본문 없음

**Response `204`**: 본문 없음

**규칙**
- 서버는 사용한 토큰을 만료 시각까지 무효 처리한다. (ERD 메모: 인메모리 관리, 별도 테이블 없음)
- 로그아웃한 토큰으로 다시 요청하면 401을 내려준다.
- 프론트는 저장한 토큰을 지우고 로그인 화면으로 이동한다.

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 토큰 없음 / 유효하지 않음 / 이미 로그아웃한 토큰 |

#### 4. 내 정보 조회
`GET /api/users/me` · 권한: 로그인

**Response `200`**
```json
{
  "userId": 1, "email": "user@gmail.com", "name": "홍길동", "nickname": "길동이",
  "generation": 14, "part": "BE"
}
```

**규칙**: 비밀번호는 절대 응답에 넣지 않는다.

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 토큰 없음 / 유효하지 않음 / 로그아웃한 토큰 |

---

### 스터디 모집글 (5~9)

#### 5. 모집글 작성
`POST /api/study-posts` · 권한: 로그인

**Request Body**
| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| title | String | O | 1~50자 |
| content | String | O | 1~2000자 |

```json
{ "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }
```

**Response `201`**
```json
{ "postId": 10 }
```

**규칙**
- 처음 만들면 `studyStatus`는 `RECRUITING`이다.
- 작성자는 토큰의 사용자다.
- 공백만 있는 문자열(`"   "`)은 허용하지 않는다. (400 `INVALID_INPUT`)

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 검증 실패 |
| 401 | `UNAUTHORIZED` | 토큰 없음 / 유효하지 않음 |

#### 6. 모집글 목록
`GET /api/study-posts` · 권한: 공개

예: `/api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10`

**Query**
| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| keyword | String | X | - | 제목 또는 내용에 포함된 키워드 |
| status | String(enum) | X | 전체 | `RECRUITING` 또는 `CLOSED` |
| part | String(enum) | X | 전체 | `PM`, `DE`, `BE`, `FE`. **작성자의 파트**가 같은 글만 |
| page | Integer | X | 0 | 페이지 번호 (0부터) |
| size | Integer | X | 10 | 페이지 크기 |

**Response `200`** (최신순 정렬)
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

**규칙**
- `part`는 글 작성자의 파트로 거르므로 StudyPost와 User를 조인한다.
- `keyword`, `status`, `part`는 함께 쓸 수 있고, 함께 쓰면 모두 만족하는 글만 나온다. (AND)
- `commentCount`는 해당 글의 댓글 수다.
- `keyword`가 빈 문자열이거나 공백만 있으면 키워드 필터를 적용하지 않는다.

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | `status`나 `part` 값이 올바르지 않음, `page`/`size`가 범위를 벗어남 |

#### 7. 모집글 상세
`GET /api/study-posts/{postId}` · 권한: 공개 (토큰이 있으면 `isAuthor` 판단에 사용)

**Path**: `postId` (Long)

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

**규칙**: 로그인하지 않았으면 `isAuthor`는 `false`다.

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 404 | `STUDY_POST_NOT_FOUND` | 글이 없음 |

#### 8. 모집글 수정
`PATCH /api/study-posts/{postId}` · 권한: 작성자

**Path**: `postId` (Long)

**Request Body** (바꿀 필드만 보낸다)
| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| title | String | X | 보내면 1~50자 |
| content | String | X | 보내면 1~2000자 |
| studyStatus | String(enum) | X | `RECRUITING` 또는 `CLOSED` |

```json
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }
```

**Response `200`**
```json
{ "postId": 10 }
```

**규칙**
- 모집 상태는 RECRUITING ↔ CLOSED 자유롭게 바꿀 수 있다.
- 보내지 않은(또는 `null`인) 필드는 바꾸지 않는다. 아무 필드도 없는 요청(`{}`)은 변경 없이 200이다.
- 보낸 필드가 빈 값이거나 공백만 있으면(`"title": ""`) 400 `INVALID_INPUT`이다.

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 검증 실패 |
| 401 | `UNAUTHORIZED` | 토큰 없음 / 유효하지 않음 (과제 목록에 없지만 추가) |
| 403 | `NOT_POST_AUTHOR` | 작성자가 아님 |
| 404 | `STUDY_POST_NOT_FOUND` | 글이 없음 |

확인 순서는 공통 규칙의 [작성자 확인 순서](#작성자-확인-순서)를 따른다. (401 → 404 → 403)

#### 9. 모집글 삭제
`DELETE /api/study-posts/{postId}` · 권한: 작성자

**Path**: `postId` (Long)

**Response `204`**: 본문 없음

**규칙**: 글을 삭제하면 달린 댓글도 함께 삭제한다. (ERD 메모: DB cascade가 아닌 코드에서 삭제)

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 토큰 없음 / 유효하지 않음 (과제 목록에 없지만 추가) |
| 403 | `NOT_POST_AUTHOR` | 작성자가 아님 |
| 404 | `STUDY_POST_NOT_FOUND` | 글이 없음 |

확인 순서: 401 → 404 → 403

---

### 댓글 = 스터디 신청 (10~13)

#### 10. 댓글 작성
`POST /api/study-posts/{postId}/comments` · 권한: 로그인

**Path**: `postId` (Long)

**Request Body**
| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| content | String | O | 1~500자, 공백만 있으면 안 됨 |

```json
{ "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요." }
```

**Response `201`**
```json
{ "commentId": 5 }
```

**규칙**
- 모집이 마감된(`CLOSED`) 글에는 댓글을 쓸 수 없다.
- 같은 사람이 여러 번 댓글을 달아도 된다.

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 검증 실패 |
| 401 | `UNAUTHORIZED` | 토큰 없음 / 유효하지 않음 |
| 404 | `STUDY_POST_NOT_FOUND` | 글이 없음 |
| 409 | `STUDY_POST_CLOSED` | 마감된 글 |

#### 11. 댓글 목록
`GET /api/study-posts/{postId}/comments` · 권한: 공개 (토큰이 있으면 `isAuthor` 판단에 사용)

예: `/api/study-posts/10/comments?page=0&size=20`

**Path**: `postId` (Long)

**Query**
| 이름 | 타입 | 필수 | 기본값 |
| --- | --- | --- | --- |
| page | Integer | X | 0 |
| size | Integer | X | 20 |

**Response `200`** (오래된 순 정렬)
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

**규칙**: 로그인하지 않았으면 `isAuthor`는 `false`다. (7번과 동일)

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | `page`/`size`가 범위를 벗어남 (과제 목록에 없지만 추가) |
| 404 | `STUDY_POST_NOT_FOUND` | 글이 없음 |

#### 12. 댓글 수정
`PATCH /api/comments/{commentId}` · 권한: 작성자

**Path**: `commentId` (Long)

**Request Body**
| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| content | String | O | 1~500자, 공백만 있으면 안 됨 (10번과 동일) |

```json
{ "content": "수정한 신청 메시지" }
```

**Response `200`**
```json
{ "commentId": 5 }
```

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 검증 실패 |
| 401 | `UNAUTHORIZED` | 토큰 없음 / 유효하지 않음 (과제 목록에 없지만 추가) |
| 403 | `NOT_COMMENT_AUTHOR` | 작성자가 아님 |
| 404 | `COMMENT_NOT_FOUND` | 댓글이 없음 |

확인 순서: 401 → 404 → 403

#### 13. 댓글 삭제
`DELETE /api/comments/{commentId}` · 권한: 작성자

**Path**: `commentId` (Long)

**Response `204`**: 본문 없음

**에러**
| HTTP | 상수 | 상황 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 토큰 없음 / 유효하지 않음 (과제 목록에 없지만 추가) |
| 403 | `NOT_COMMENT_AUTHOR` | 작성자가 아님 |
| 404 | `COMMENT_NOT_FOUND` | 댓글이 없음 |

확인 순서: 401 → 404 → 403

---

## 3. 에러 코드 표

- 상수 이름과 `code` 값은 과제 표를 그대로 쓴다.
- `message`는 기존 `ErrorCode`와 같은 "~습니다." 문체로 쓴다. (아래 문구는 초안이다. 문체 규칙에 맞춰 작성했고 과제 원문의 문구는 아니다)
- `INVALID_INPUT`은 기존 `COMMON_BAD_REQUEST`와 별도로 추가한다. 요청 값 검증 실패는 `INVALID_INPUT`을 쓴다.

| HTTP | ErrorCode 상수 | 응답 `code` | message (초안) | 사용 API |
| --- | --- | --- | --- | --- |
| 400 | `INVALID_INPUT` | `COMMON-003` | 입력값이 올바르지 않습니다. | 1, 2, 5, 6, 8, 10, 11, 12 |
| 401 | `UNAUTHORIZED` | `COMMON-004` | 인증이 필요합니다. | 3, 4, 5, 8, 9, 10, 12, 13 |
| 409 | `EMAIL_ALREADY_EXISTS` | `USER-001` | 이미 가입된 이메일입니다. | 1 |
| 401 | `INVALID_CREDENTIALS` | `USER-002` | 이메일 또는 비밀번호가 올바르지 않습니다. | 2 |
| 404 | `STUDY_POST_NOT_FOUND` | `STUDY-001` | 스터디 모집글을 찾을 수 없습니다. | 7, 8, 9, 10, 11 |
| 403 | `NOT_POST_AUTHOR` | `STUDY-002` | 모집글 작성자만 수정하거나 삭제할 수 있습니다. | 8, 9 |
| 409 | `STUDY_POST_CLOSED` | `STUDY-003` | 모집이 마감된 글에는 댓글을 작성할 수 없습니다. | 10 |
| 404 | `COMMENT_NOT_FOUND` | `COMMENT-001` | 댓글을 찾을 수 없습니다. | 12, 13 |
| 403 | `NOT_COMMENT_AUTHOR` | `COMMENT-002` | 댓글 작성자만 수정하거나 삭제할 수 있습니다. | 12, 13 |

---

## 4. 기존 코드와의 관계

기존 `ErrorCode.java`
| 코드 | 상수 | HTTP |
| --- | --- | --- |
| COMMON-001 | COMMON_BAD_REQUEST | 400 |
| COMMON-002 | COMMON_INTERNAL_SERVER_ERROR | 500 |
| BOOK-001 | BOOK_NOT_FOUND | 404 |

**겹치거나 충돌하는 부분**
1. `INVALID_INPUT`(400)과 `COMMON_BAD_REQUEST`(400, COMMON-001)는 의미가 거의 같다. → **둘 다 유지**한다. 요청 값 검증 실패는 `INVALID_INPUT`, 기존 상수는 그대로 둔다.
2. 상수 이름 규칙: `architecture.md`는 `도메인_상황`(예: `COMMON_BAD_REQUEST`)인데, 과제 상수 `INVALID_INPUT`, `UNAUTHORIZED`, `INVALID_CREDENTIALS`, `EMAIL_ALREADY_EXISTS`, `NOT_POST_AUTHOR`, `NOT_COMMENT_AUTHOR`는 도메인 prefix로 시작하지 않는다. → 과제 상수 이름을 따르고 **규칙의 예외로 본다.** (`architecture.md`에 예외를 적을지는 별도로 정한다)
3. `code` 번호: `COMMON-003`, `COMMON-004`는 기존 001, 002 다음 번호라 충돌 없음. `USER-`, `STUDY-`, `COMMENT-`는 새 prefix라 충돌 없음.
4. `message` 문체: 과제 예시는 "~이에요."지만 → 기존과 같은 **"~습니다."**로 통일한다.

**구현 단계에서 처리할 것 (이 문서 작업에서는 코드 수정 안 함)**
- `GlobalExceptionHandler`는 지금 `Backend14thException`과 `Exception`만 처리한다. `@Valid` 실패(`MethodArgumentNotValidException`), 잘못된 enum 쿼리(`status=FOO` → 타입 변환 실패), 잘못된 JSON 본문은 지금 그대로면 **500**이 된다. 400 `INVALID_INPUT`으로 바꾸는 핸들러가 필요하다.
- Spring Security 필터에서 나는 401/403은 `@RestControllerAdvice`를 거치지 않는다. `ErrorResponse` 형식을 맞추려면 `AuthenticationEntryPoint`(필요하면 `AccessDeniedHandler`)가 필요하다.
- `ResponseUtils`에 204용 메서드가 없다.
- `SecurityConfig`는 현재 `anyRequest().permitAll()`이다. 인증 구현 시 바꿔야 하지만 CLAUDE.md상 임의로 바꾸지 않고 따로 합의 후 진행한다.

---

## 5. 결정 사항

| # | 항목 | 관련 위치 |
| --- | --- | --- |
| ~~Q1~~ | 날짜 형식 → ISO | 공통 규칙 |
| ~~Q2~~ | 에러 `code` → 과제 예시 그대로 | 에러 코드 표 |
| ~~Q3~~ | 에러 `message` 문체 → "~습니다." (문구는 초안) | 에러 코드 표 |
| ~~Q4~~ | `INVALID_INPUT` → 기존 상수와 둘 다 유지 | 4절 |
| ~~Q5~~ | 작성자 API에 401 → 넣는다 | 8, 9, 12, 13 |
| ~~Q6~~ | 판정 순서 → 401 → 404 → 403 | 공통 규칙 |
| ~~Q7~~ | 로그인 빈 값 → 400 `INVALID_INPUT` | 2 |
| ~~Q8~~ | name 비어 있지만 않으면 됨, password 최대 없음, nickname 중복 허용 | 1 |
| ~~Q9~~ | page/size → 범위 밖이면 400, size 최대 100 | 공통 규칙, 6, 11 |
| ~~Q10~~ | keyword 빈 값/공백 → 필터 없음 | 6 |
| ~~Q11~~ | 댓글 목록 비로그인 `isAuthor` → false | 11 |
| ~~Q12~~ | `{}` → 변경 없이 200, 보낸 필드가 빈 값이면 400. 댓글 수정은 10번과 같은 검증 | 8, 12 |
| ~~Q13~~ | 공백만 있는 문자열 → 400 | 5, 8, 10, 12 |
