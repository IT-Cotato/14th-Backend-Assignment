# API 명세

- 모든 주소는 `/api`로 시작
- 인증: `Authorization: Bearer {accessToken}`
- 성공 응답은 3주차 공통 응답으로 감쌈. 204는 본문 없음
- 실패 응답: success, status, code, message, method, path, timestamp

## 목록
| # | Method | URL | 권한 |
|---|---|---|---|
| 1 | POST | /api/auth/signup | 공개 |
| 2 | POST | /api/auth/login | 공개 |
| 3 | POST | /api/auth/logout | 로그인 |
| 4 | GET | /api/users/me | 로그인 |
| 5 | POST | /api/study-posts | 로그인 |
| 6 | GET | /api/study-posts | 공개 |
| 7 | GET | /api/study-posts/{postId} | 공개 |
| 8 | PATCH | /api/study-posts/{postId} | 작성자 |
| 9 | DELETE | /api/study-posts/{postId} | 작성자 |
| 10 | POST | /api/study-posts/{postId}/comments | 로그인 |
| 11 | GET | /api/study-posts/{postId}/comments | 공개 |
| 12 | PATCH | /api/comments/{commentId} | 작성자 |
| 13 | DELETE | /api/comments/{commentId} | 작성자 |
