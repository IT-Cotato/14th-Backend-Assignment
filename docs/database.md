# 데이터베이스

## 설정 방식

- DB는 MySQL을 사용합니다.
- 접속 정보는 프로젝트 루트의 `.env`에 있고 Git에 올리지 않습니다.
- `application.yml`이 `.env`를 불러오고, `application-local.yml`이 다음 환경변수를 참조합니다.
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`

`.env`는 `optional:file:.env[.properties]`로 불러오고, `DB_URL`에는 기본값이 없습니다.

## 스키마 관리

- `local` 프로필은 `ddl-auto: update`를 사용합니다.
- 별도의 마이그레이션 도구는 없습니다.

## 테스트와 DB

`@SpringBootTest`는 현재 `local` 프로필로 실행됩니다. 그래서 다음 사항에 주의합니다.

- 테스트와 빌드에 로컬 MySQL과 `.env`가 필요합니다.
- 테스트를 실행하면 `ddl-auto: update`가 실제 로컬 DB에 적용될 수 있습니다.

## 엔티티 변경 절차

1. 변경할 컬럼과 기존 데이터에 미치는 영향을 정리합니다.
2. 엔티티와 관련 테스트를 함께 작성합니다.
3. 컬럼 삭제나 타입 변경은 사용자와 절차를 먼저 정합니다.
4. 테스트를 실행하고 결과를 PR에 적습니다.
