# ERD

ERDCloud: https://www.erdcloud.com/d/ua8yoNMr496mMjK6p

![ERD](erd.png)

## 관계

- `users` 1 : N `study_post` (작성자)
- `study_post` 1 : N `study_post_comment` (모집글의 댓글, 곧 스터디 신청)
- `users` 1 : N `study_post_comment` (댓글 작성자)

모두 비식별 관계이고, 연관관계는 `LAZY`, Enum은 문자열로 저장한다.

## users

| 컬럼 | Type | 제약 |
| --- | --- | --- |
| id | BIGINT | PK, 자동 증가 |
| email | VARCHAR(255) | UK, NOT NULL |
| password | VARCHAR(255) | NOT NULL (BCrypt) |
| name | VARCHAR(50) | NOT NULL |
| nickname | VARCHAR(10) | NOT NULL |
| generation | INT | NOT NULL |
| part | VARCHAR(10) | NOT NULL (PM, DE, BE, FE) |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

## study_post

| 컬럼 | Type | 제약 |
| --- | --- | --- |
| id | BIGINT | PK, 자동 증가 |
| user_id | BIGINT | FK → users.id, NOT NULL |
| title | VARCHAR(50) | NOT NULL |
| content | VARCHAR(2000) | NOT NULL |
| study_status | VARCHAR(20) | NOT NULL (RECRUITING, CLOSED) |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

## study_post_comment

| 컬럼 | Type | 제약 |
| --- | --- | --- |
| id | BIGINT | PK, 자동 증가 |
| study_post_id | BIGINT | FK → study_post.id, NOT NULL |
| user_id | BIGINT | FK → users.id, NOT NULL |
| content | VARCHAR(500) | NOT NULL |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |
