# 스터디 매칭 API

성공은 기존 `ApiResponse`의 `data`로 감싼다. 204는 본문이 없다. 실패는 `ErrorResponse`의 success, status, code, message, method, path, timestamp를 사용한다.
로그인 및 작성자 API는 `Authorization: Bearer <accessToken>`이 필요하다.

| # | 메서드와 경로 | 권한 | 요청 | 성공 |
| --- | --- | --- | --- | --- |
| 1 | POST /api/auth/signup | 공개 | email, password, name, nickname, generation, part | 201 userId |
| 2 | POST /api/auth/login | 공개 | email, password | 200 accessToken, tokenType |
| 3 | POST /api/auth/logout | 로그인 | 없음 | 204 |
| 4 | GET /api/users/me | 로그인 | 없음 | 200 userId, email, name, nickname, generation, part |
| 5 | POST /api/study-posts | 로그인 | title, content | 201 postId |
| 6 | GET /api/study-posts | 공개 | keyword, status, part, page=0, size=10 | 200 모집글 페이지 |
| 7 | GET /api/study-posts/{postId} | 공개 | 없음 | 200 모집글 상세 |
| 8 | PATCH /api/study-posts/{postId} | 작성자 | title, content, studyStatus 중 변경할 항목 | 200 postId |
| 9 | DELETE /api/study-posts/{postId} | 작성자 | 없음 | 204 (댓글도 삭제) |
| 10 | POST /api/study-posts/{postId}/comments | 로그인 | content | 201 commentId |
| 11 | GET /api/study-posts/{postId}/comments | 공개 | page=0, size=20 | 200 댓글 페이지 |
| 12 | PATCH /api/comments/{commentId} | 작성자 | content | 200 commentId |
| 13 | DELETE /api/comments/{commentId} | 작성자 | 없음 | 204 |

- 페이지는 content, page, size, totalElements, totalPages를 반환한다. size는 1~100이다.
- 작성자는 nickname, generation, part를 담은 author 객체로 반환한다.
- 모집글 목록 항목은 postId, title, studyStatus, author, commentCount, createdAt을 반환한다. 제목·내용 키워드, 상태, 작성자 파트를 AND 조건으로 적용하고 최신순으로 정렬한다.
- 모집글 상세는 postId, title, content, studyStatus, author, isAuthor, createdAt, updatedAt을 반환한다. 익명 사용자의 isAuthor는 false다.
- 댓글 항목은 commentId, content, author, isAuthor, createdAt을 반환하며 오래된 순으로 정렬한다.
- 기수는 양의 정수, 파트는 PM·DE·BE·FE, 닉네임은 2~10자다. 비밀번호는 8자 이상이며 BCrypt의 72바이트 제한을 적용한다.
- 제목은 1~50자, 모집글 본문은 1~2000자, 댓글 본문은 1~500자다. 모집 상태는 RECRUITING 또는 CLOSED다. 초기 상태는 RECRUITING이다.
- 로그아웃은 JWT의 jti와 만료 시각을 revoked_token에 저장한다. 같은 토큰의 재사용은 401이다.

| 상태 | 상수 | 응답 code |
| --- | --- | --- |
| 400 | INVALID_INPUT | COMMON-003 |
| 401 | UNAUTHORIZED | COMMON-004 |
| 401 | INVALID_CREDENTIALS | USER-002 |
| 403 | NOT_POST_AUTHOR | STUDY-002 |
| 403 | NOT_COMMENT_AUTHOR | COMMENT-002 |
| 404 | STUDY_POST_NOT_FOUND | STUDY-001 |
| 404 | COMMENT_NOT_FOUND | COMMENT-001 |
| 409 | EMAIL_ALREADY_EXISTS | USER-001 |
| 409 | STUDY_POST_CLOSED | STUDY-003 |
