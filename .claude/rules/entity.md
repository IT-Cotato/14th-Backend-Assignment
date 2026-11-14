---
paths:
  - "backend/src/main/java/**/domain/**/entity/*.java"
---

# 엔티티 규칙

## 작성
- `global.common.entity.BaseEntity`를 상속합니다.
- ID는 `@GeneratedValue(strategy = GenerationType.IDENTITY)`로 생성합니다.
- `created_at`, `updated_at` 필드를 엔티티에 다시 선언하지 않습니다.

## 명세 규칙 (`docs/api-spec.md` 2장)
- 연관관계는 모두 `fetch = FetchType.LAZY`로 설정합니다.
- Enum은 `@Enumerated(EnumType.STRING)`으로 저장합니다.
- 비밀번호는 BCrypt로 암호화해서 저장합니다.
- 명세에 적힌 테이블과 컬럼의 이름과 의미는 바꾸지 않습니다.
- 컬럼이나 테이블을 추가하면 ERD, `docs/api-spec.md`, PR의 "ERD/문서 변경"에 함께 적습니다.

## 연관관계
- 연관 방향(단방향/양방향)과 cascade 방식은 추가하기 전에 사용자와 정합니다.

## 조회와 삭제
- 목록에서 연관 엔티티를 쓰면 `join fetch`로 함께 가져오고, 개수는 `group by` 쿼리 한 번으로 셉니다. (N+1 금지)
- 페이지 쿼리에 `join fetch`를 쓰면 `countQuery`를 따로 적습니다.
- 자식을 JPQL 벌크 삭제(`@Modifying`)한 뒤 부모를 지울 때는 `clearAutomatically`를 쓰지 않습니다. (부모가 detach됩니다)
- 테스트에서 자식을 저장한 같은 영속성 컨텍스트로 벌크 삭제를 검증하면 `TransientPropertyValueException`이 납니다. `flush()` 뒤에 `clear()`해서 실제 요청과 같은 상태로 만듭니다.
- 리포지토리 쿼리 테스트는 `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)`(로컬 MySQL, 롤백)로 작성하고, UUID로 데이터 범위를 좁힙니다.

## 변경 절차
- 관련 테스트를 함께 작성합니다.
- 변경 전에 추가, 삭제, 변경되는 컬럼과 기존 데이터에 미치는 영향을 사용자에게 알립니다.
- 테스트를 실행하기 전에 `ddl-auto: update`가 로컬 DB에 적용될 수 있음을 사용자에게 알립니다.
- 컬럼 삭제나 타입 변경은 절차를 사용자와 먼저 정합니다.
- 자세한 절차는 `docs/database.md`를 따릅니다.
