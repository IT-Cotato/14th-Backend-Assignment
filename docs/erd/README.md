# ERD

- ERDCloud: https://www.erdcloud.com/d/NbDTNsyp6XmpHzkNf
- 이미지: [erd.png](./erd.png)

![ERD](./erd.png)

## 테이블

| 테이블 | 설명 | 주요 컬럼 |
|---|---|---|
| `user` | 회원 | `email`(UK), `password`(BCrypt), `name`, `nickname`, `generation`(기수, 1 이상), `part`(PM, DE, BE, FE) |
| `study_post` | 스터디 모집글 | `user_id`(FK → user), `title`, `content`, `study_status`(RECRUITING, CLOSED) |
| `study_post_comment` | 모집글 댓글 = 스터디 신청 | `study_post_id`(FK → study_post), `user_id`(FK → user), `content` |

모든 테이블에 `created_at`, `updated_at`이 있어요. (`BaseEntity`가 자동으로 채워요)

## 관계 (모두 1:N)

- `user` 1 : N `study_post`: 한 사람이 모집글을 여러 개 써요.
- `user` 1 : N `study_post_comment`: 한 사람이 댓글을 여러 개 써요.
- `study_post` 1 : N `study_post_comment`: 한 모집글에 댓글이 여러 개 달려요. 모집글을 지우면 댓글도 함께 지워요.

## 설계 메모

- `part`, `study_status`는 Enum을 문자열(`VARCHAR`)로 저장해요. 숫자(ORDINAL)로 저장하면 Enum 순서가 바뀔 때 데이터가 꼬여요.
- 컬럼 길이(`title` 50, `content` 2000, 댓글 `content` 500, `nickname` 10)는 API 명세의 입력 검증 규칙에 맞췄어요.
- 연관관계는 모두 `LAZY`로 가져와요.
- 로그아웃한 토큰은 테이블을 만들지 않고 서버 메모리에 저장해요. 명세에 "저장 방식은 자유"라고 되어 있고, 테이블을 늘리지 않고 단순하게 가기 위해서예요. (서버를 재시작하면 목록이 사라지지만, 그때는 토큰 만료 시간 안의 토큰도 다시 쓸 수 있게 되는 한계가 있어요.)
