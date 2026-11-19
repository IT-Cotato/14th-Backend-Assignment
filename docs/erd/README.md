# ERD

- ERDCloud: https://www.erdcloud.com/d/vtdkoCKh5wsAjTdmQ
- 이미지: `erd.png`
- DDL: `schema.sql` (ERDCloud에서 SQL import로 불러올 수 있다)

![ERD](erd.png)

## 테이블

| 테이블 | 설명 | 관계 |
| --- | --- | --- |
| `users` | 회원. 기수(`generation`)와 파트(`part`)를 가입 때 본인이 입력한다 | |
| `study_post` | 스터디 모집글. `study_status`는 `RECRUITING`/`CLOSED` | `users` 1 : N `study_post` |
| `study_post_comment` | 모집글 댓글 = 스터디 신청 | `study_post` 1 : N, `users` 1 : N |
| `blacklisted_token` | 로그아웃한 토큰의 해시와 만료 시각 (과제 명세 외 추가) | 독립 |

- 과제의 `User` 테이블은 `users`로 만들었다. 컬럼 이름과 의미는 명세와 같다.
- Enum(`part`, `study_status`)은 문자열로 저장한다.
- JPA 연관관계는 모두 `LAZY`다.
- 모집글을 삭제하면 서비스에서 댓글을 먼저 지운 뒤 글을 지운다.
- `blacklisted_token`은 토큰 원문 대신 SHA-256 해시를 저장해서 DB가 유출돼도 토큰을 재사용할 수 없다. 회원과 연결하지 않은 이유는 토큰 해시만으로 조회하기 때문이다.
