# ERD

코테이토 스터디 매칭 플랫폼의 데이터 모델이다.

- ERDCloud: https://www.erdcloud.com/p/av67KMPWaKTzyn44c
- 이미지: `erd.png`

![ERD](erd.png)

## 테이블

테이블 이름은 DB에 만들어지는 이름(snake_case)이다. 엔티티는 각각 `User`, `StudyPost`, `StudyPostComment`, `RevokedToken`이다.

### user

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `id` | BIGINT | PK |
| `email` | VARCHAR(255) | NOT NULL, UK |
| `password` | VARCHAR(255) | NOT NULL (BCrypt 해시) |
| `name` | VARCHAR(50) | NOT NULL |
| `nickname` | VARCHAR(10) | NOT NULL |
| `generation` | INT | NOT NULL |
| `part` | VARCHAR(10) | NOT NULL (`PM`, `DE`, `BE`, `FE`) |
| `created_at` | DATETIME | NOT NULL |
| `updated_at` | DATETIME | NOT NULL |

### study_post

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `id` | BIGINT | PK |
| `user_id` | BIGINT | FK → `user.id`, NOT NULL |
| `title` | VARCHAR(50) | NOT NULL |
| `content` | VARCHAR(2000) | NOT NULL |
| `study_status` | VARCHAR(20) | NOT NULL (`RECRUITING`, `CLOSED`) |
| `created_at` | DATETIME | NOT NULL |
| `updated_at` | DATETIME | NOT NULL |

### study_post_comment

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `id` | BIGINT | PK |
| `user_id` | BIGINT | FK → `user.id`, NOT NULL |
| `study_post_id` | BIGINT | FK → `study_post.id`, NOT NULL |
| `content` | VARCHAR(500) | NOT NULL |
| `created_at` | DATETIME | NOT NULL |
| `updated_at` | DATETIME | NOT NULL |

댓글이 곧 스터디 신청이다. 누가(`user_id`) 어느 모집글(`study_post_id`)에 신청했는지가 한 행에 담긴다.

### revoked_token

과제의 최소 구성에 추가한 테이블이다.

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `id` | BIGINT | PK |
| `jti` | VARCHAR(255) | NOT NULL, UK |
| `expires_at` | DATETIME | NOT NULL |

## 관계

| 부모 | 자식 | 관계 | 설명 |
|---|---|---|---|
| `user` | `study_post` | 1:N | 한 사용자가 여러 모집글을 쓴다 |
| `user` | `study_post_comment` | 1:N | 한 사용자가 여러 댓글을 쓴다 |
| `study_post` | `study_post_comment` | 1:N | 한 모집글에 여러 댓글이 달린다. 모집글을 삭제하면 댓글도 함께 삭제한다 |

세 관계 모두 FK가 자식의 PK가 아닌 일반 컬럼인 비식별 관계다. `revoked_token`은 다른 테이블과 관계가 없다.

## 규칙

- 연관관계는 모두 `LAZY`로 하고, Enum(`part`, `study_status`)은 문자열로 저장한다.
- 생성·수정 시각(`created_at`, `updated_at`)은 `BaseEntity`로 관리한다.

## 추가한 테이블: revoked_token

JWT는 서버가 상태를 저장하지 않아서, 로그아웃한 토큰을 서버가 따로 기억해야 한다. 토큰 전체 대신 토큰 고유 ID(`jti`)와 만료 시각을 저장하고, 요청마다 `jti`가 이 테이블에 있으면 `401`을 내려준다. 만료 시각이 지난 행은 더 이상 필요 없다.
자세한 규칙은 `docs/api-spec.md`의 "추가 설계"를 참고한다.
