# API 명세

`docs/PRD.md` 5장(공통 규칙)과 6장(API 명세 13개)을 그대로 정리한 문서입니다. 필드명/상태코드/에러코드는 PRD 원문을 그대로 따릅니다.

## 공통 규칙 (백엔드)

- 모든 API 주소는 `/api`로 시작합니다.
- 인증이 필요한 API는 `Authorization: Bearer 토큰` 헤더를 사용합니다.
- 권한은 **공개**(인증 불필요) / **로그인**(토큰 필요) 두 단계이며, 수정·삭제는 추가로 **작성자 본인**이어야 합니다.
- 성공 응답: 3주차의 공통 응답(`ApiResponse`)으로 감쌉니다. 아래 각 API 명세의 응답은 `data` 안쪽만 적은 것입니다. 본문 없는 `204`는 감싸지 않고 그대로 반환합니다.
- 실패 응답: 모든 API에서 3주차 `ErrorResponse` 형식을 사용합니다.

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

- `code`는 `도메인prefix-일련번호`(예: `STUDY-001`) 형식입니다. 아래 명세에 나오는 `STUDY_POST_NOT_FOUND` 등은 **`ErrorCode` enum 상수 이름**이며, 3주차의 `BOOK_NOT_FOUND` ↔ `BOOK-001` 관계와 동일하게 매핑합니다.
- `ErrorCode`는 3주차처럼 enum 하나에 모으고, 새 도메인은 새 그룹으로 추가합니다.
- 페이지 응답 공통 필드: `content`, `page`, `size`, `totalElements`, `totalPages`.
- 작성자 정보는 `author` 객체로 표현하며 `nickname`, `generation`, `part` 필드를 가집니다.
- 명세에 없는 세부 규칙은 "쉬운 쪽"으로 구현합니다:
  - 모집 상태는 RECRUITING ↔ CLOSED 자유 전환
  - 같은 사람이 댓글(신청)을 여러 번 작성 가능
  - 토큰은 `accessToken` 하나만 사용(리프레시 토큰 없음)
- API 명세에 없는 필드를 추가하는 것은 괜찮지만, 명세에 있는 필드는 그대로 지킵니다.
- 인증 실패(토큰 없음/무효/만료/로그아웃된 토큰)는 모두 `401 UNAUTHORIZED`입니다.
- 인증/인가 예외(401, 403)도 `ErrorResponse` 형식으로 나가도록 Security의 `AuthenticationEntryPoint`, `AccessDeniedHandler`를 설정합니다.
- CORS: 프론트 `http://localhost:3000`을 허용합니다.

## 인증 (Auth)

### 1. 회원가입 — `POST /api/auth/signup` (공개)

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

Response `201`:
```json
{ "userId": 1 }
```

검증: email 형식 / password 8자 이상 / nickname 2~10자 / generation 1 이상 정수 / part는 PM, DE, BE, FE 중 하나

에러: `400 INVALID_INPUT`, `409 EMAIL_ALREADY_EXISTS`

### 2. 로그인 — `POST /api/auth/login` (공개)

Request:
```json
{ "email": "user@gmail.com", "password": "password1234" }
```

Response `200`:
```json
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer" }
```

에러: `401 INVALID_CREDENTIALS`

### 3. 로그아웃 — `POST /api/auth/logout` (로그인)

Response `204`: 본문 없음

규칙:
- 서버는 사용한 토큰을 만료 시각까지 무효 처리합니다(저장 방식은 자유: DB, 인메모리).
- 로그아웃한 토큰으로 다시 요청하면 `401`을 반환합니다.
- 프론트는 저장한 토큰을 지우고 로그인 화면으로 이동합니다.

에러: `401 UNAUTHORIZED`

### 4. 내 정보 조회 — `GET /api/users/me` (로그인)

Response `200`:
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

규칙: 비밀번호는 절대 응답에 넣지 않습니다.

에러: `401 UNAUTHORIZED`

## 스터디 모집글 (Study Post)

### 5. 모집글 작성 — `POST /api/study-posts` (로그인)

Request:
```json
{ "title": "알고리즘 스터디 모집", "content": "주 2회 백준 풀이..." }
```

Response `201`:
```json
{ "postId": 10 }
```

검증: title 1~50자 / content 1~2000자

규칙: 처음 만들면 `study_status`는 `RECRUITING`

에러: `400 INVALID_INPUT`, `401 UNAUTHORIZED`

### 6. 모집글 목록 — `GET /api/study-posts?keyword=알고리즘&status=RECRUITING&part=BE&page=0&size=10` (공개)

쿼리 파라미터:
- `keyword` (선택, title/content 포함 검색)
- `status` (선택, `RECRUITING`|`CLOSED`, 없으면 전체)
- `part` (선택, `PM`|`DE`|`BE`|`FE` — 작성자의 part로 필터, 없으면 전체)
- `page` (선택, 기본 0)
- `size` (선택, 기본 10)

Response `200` (최신순):
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

규칙: `part`는 모집글 작성자의 part로 필터링합니다(StudyPost와 User를 join). keyword/status/part는 조합 가능하며 AND 조건입니다.

에러: `400 INVALID_INPUT` (status나 part 값이 올바르지 않을 때)

구현 참고: 동적 조건은 QueryDSL/Specification/JPQL 등 편한 방식으로 구현합니다. `commentCount`와 `author` 조회 시 N+1이 발생하지 않도록 fetch join 또는 배치 조회로 처리하고 테스트로 검증합니다.

### 7. 모집글 상세 — `GET /api/study-posts/{postId}` (공개)

Response `200`:
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

규칙: 로그인하지 않았으면 `isAuthor`는 `false`입니다. 공개 API지만 토큰이 있으면 파싱해서 `isAuthor`를 계산합니다(soft-auth).

에러: `404 STUDY_POST_NOT_FOUND`

### 8. 모집글 수정 — `PATCH /api/study-posts/{postId}` (작성자 본인)

Request (바꿀 필드만):
```json
{ "title": "수정된 제목", "content": "수정된 내용", "studyStatus": "CLOSED" }
```

Response `200`:
```json
{ "postId": 10 }
```

에러: `400 INVALID_INPUT`, `403 NOT_POST_AUTHOR`, `404 STUDY_POST_NOT_FOUND`

### 9. 모집글 삭제 — `DELETE /api/study-posts/{postId}` (작성자 본인)

Response `204`: 본문 없음

규칙: 글을 삭제하면 달린 댓글도 함께 삭제합니다(cascade 또는 명시적 삭제).

에러: `403 NOT_POST_AUTHOR`, `404 STUDY_POST_NOT_FOUND`

## 댓글 = 스터디 신청 (Comment)

### 10. 댓글 작성 — `POST /api/study-posts/{postId}/comments` (로그인)

Request:
```json
{ "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요." }
```

Response `201`:
```json
{ "commentId": 5 }
```

검증: content 1~500자

규칙: 모집이 마감된(`CLOSED`) 글에는 댓글을 쓸 수 없습니다.

에러: `400 INVALID_INPUT`, `401 UNAUTHORIZED`, `404 STUDY_POST_NOT_FOUND`, `409 STUDY_POST_CLOSED`

### 11. 댓글 목록 — `GET /api/study-posts/{postId}/comments?page=0&size=20` (공개)

Response `200` (오래된 순):
```json
{
  "content": [
    {
      "commentId": 5,
      "content": "안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요.",
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

에러: `404 STUDY_POST_NOT_FOUND`

### 12. 댓글 수정 — `PATCH /api/comments/{commentId}` (작성자 본인)

Request:
```json
{ "content": "수정한 신청 메시지" }
```

Response `200`:
```json
{ "commentId": 5 }
```

에러: `400 INVALID_INPUT`, `403 NOT_COMMENT_AUTHOR`, `404 COMMENT_NOT_FOUND`

### 13. 댓글 삭제 — `DELETE /api/comments/{commentId}` (작성자 본인)

Response `204`: 본문 없음

에러: `403 NOT_COMMENT_AUTHOR`, `404 COMMENT_NOT_FOUND`

## 에러 코드 요약

| HTTP | ErrorCode 상수 | 응답 code(예시) | 설명 |
| --- | --- | --- | --- |
| 400 | INVALID_INPUT | COMMON-003 | 요청 값 검증 실패 (기수가 1 미만이거나 파트가 목록에 없는 경우 포함) |
| 401 | INVALID_CREDENTIALS | USER-002 | 이메일 또는 비밀번호가 틀려요 |
| 401 | UNAUTHORIZED | COMMON-004 | 토큰이 없거나, 유효하지 않거나, 로그아웃된 토큰이에요 |
| 403 | NOT_POST_AUTHOR / NOT_COMMENT_AUTHOR | STUDY-002 / COMMENT-002 | 작성자가 아니에요 |
| 404 | STUDY_POST_NOT_FOUND / COMMENT_NOT_FOUND | STUDY-001 / COMMENT-001 | 대상이 없어요 |
| 409 | EMAIL_ALREADY_EXISTS | USER-001 | 이미 가입된 이메일이에요 |
| 409 | STUDY_POST_CLOSED | STUDY-003 | 모집이 마감된 글에는 댓글을 쓸 수 없어요 |

## 부록: 유연성 규칙

- API 명세에 없는 필드를 추가하는 것은 가능하지만, 명세에 있는 필드는 이름과 의미를 그대로 지킵니다.
- 컬럼/테이블을 추가해도 되지만 기존 이름·의미는 바꾸지 않고, 추가분은 ERD/`docs/api-spec.md`/PR 문서에 함께 기록합니다.
