---
paths:
  - "src/main/java/**/domain/**/entity/*.java"
---

# 엔티티 규칙

## 작성
- `global.common.entity.BaseEntity`를 상속합니다.
- ID는 `@GeneratedValue(strategy = GenerationType.IDENTITY)`로 생성합니다.
- `created_at`, `updated_at` 필드를 엔티티에 다시 선언하지 않습니다.

## 연관관계
- 현재 코드에는 연관관계와 fetch 설정이 없습니다.
- 연관관계를 추가하기 전에 연관 방향과 fetch 전략을 사용자와 정합니다.

## 변경 절차
- 관련 테스트를 함께 작성합니다.
- 변경 전에 추가, 삭제, 변경되는 컬럼과 기존 데이터에 미치는 영향을 사용자에게 알립니다.
- 테스트를 실행하기 전에 `ddl-auto: update`가 로컬 DB에 적용될 수 있음을 사용자에게 알립니다.
- 컬럼 삭제나 타입 변경은 절차를 사용자와 먼저 정합니다.
- 자세한 절차는 `docs/database.md`를 따릅니다.
