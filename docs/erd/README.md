# ERD: 코테이토 스터디 매칭 플랫폼

- ERDCloud: https://www.erdcloud.com/d/Mfdap8g5i5nmk5jQJ
- 컬럼 상세와 제약 조건은 `docs/api-spec.md` 의 "7. 데이터 모델"을 참고한다.

![ERD](./erd.png)

## 테이블

| 테이블 | 설명 |
| --- | --- |
| `users` | 회원. 기수(`generation`)와 파트(`part`)를 회원가입 때 입력한다. `email` 은 UNIQUE |
| `study_post` | 스터디 모집글. `study_status` 는 `RECRUITING` / `CLOSED` |
| `study_post_comment` | 모집글 댓글. 댓글을 남기는 것이 곧 스터디 신청이다 |
| `token_blacklist` | **추가한 테이블.** 로그아웃한 토큰을 원래 만료 시각(`expires_at`)까지 저장한다. `token` 은 UNIQUE |

## 관계

| 관계 | 종류 | 설명 |
| --- | --- | --- |
| `users` - `study_post` | 1 : N | 한 사람이 모집글을 여러 개 쓸 수 있다 (`study_post.user_id`) |
| `users` - `study_post_comment` | 1 : N | 한 사람이 댓글을 여러 개 쓸 수 있다 (`study_post_comment.user_id`) |
| `study_post` - `study_post_comment` | 1 : N | 한 모집글에 댓글이 여러 개 달린다 (`study_post_comment.study_post_id`) |
| `token_blacklist` | 없음 | 다른 테이블과 연결하지 않는다 |

- 연관관계는 모두 `LAZY` 로 매핑하고, Enum은 문자열로 저장한다.
- 모집글을 삭제하면 댓글도 함께 삭제한다. DB의 `ON DELETE CASCADE` 가 아니라 서비스 코드에서 처리한다.
- 테이블이 바뀌면 ERDCloud와 `erd.png`, `docs/api-spec.md` 를 함께 고친다.
