---
name: add-domain-api
description: 이 프로젝트에 새 도메인이나 API를 추가할 때 사용합니다. 패키지 구성, 응답 형식, 에러 코드, Swagger 문서화 순서를 안내합니다.
---

# 도메인 API 추가

`domain/book/`을 기준 예제로 삼아 다음 순서로 진행합니다.
아래의 `domain/` 경로는 모두 `backend/src/main/java/com/cotato/backend14th/domain/`을 뜻하고, 테스트는 `backend/src/test/java/` 아래에 둡니다. Gradle 명령은 `backend/`에서 실행합니다.

Book 예제는 service가 없으므로, 계층 구조는 `docs/architecture.md`의 "5,6주차부터 적용할 구조"를 따릅니다.

1. `docs/api-spec.md`에서 해당 API의 주소, 권한, 요청/응답 필드, 검증, 에러를 확인합니다. 명세에 없는 내용은 추측하지 않고 사용자에게 묻습니다.
2. `docs/conventions.md`, `docs/architecture.md`와 `domain/book/`의 코드를 읽습니다.
3. `domain/{도메인}/` 아래에 `controller`, `service`, `dto`, `entity`, `repository` 패키지를 만듭니다.
4. 엔티티를 작성합니다. `BaseEntity`를 상속하고 `.claude/rules/entity.md`를 따릅니다.
5. `JpaRepository`를 상속한 repository를 작성합니다.
6. 요청 DTO와 응답 DTO를 작성합니다. 명세의 검증 규칙을 검증 어노테이션으로 붙이고, 엔티티를 응답으로 직접 반환하지 않습니다.
7. 필요한 에러를 `ErrorCode`에 추가합니다. 상수 이름과 code는 `docs/api-spec.md` 8장 값을 그대로 씁니다.
8. service를 작성합니다. 권한 검사(작성자 확인)와 비즈니스 규칙을 여기에 둡니다.
9. 컨트롤러를 작성합니다. `.claude/rules/api.md`를 따릅니다.
   - 주소는 `/api`로 시작합니다.
   - 요청 DTO에 `@Valid`를 붙입니다.
   - 성공 응답은 `ResponseUtils`로 반환하고, 204는 `ResponseEntity.noContent().build()`로 반환합니다.
   - 예외는 `Backend14thException`으로 던집니다.
   - `@Tag`, `@Operation`, `@Parameter`를 붙입니다.
10. 정상과 실패 경우의 테스트를 작성합니다.
11. 실행 전에 테스트가 로컬 DB에 영향을 줄 수 있음을 사용자에게 알리고 테스트를 실행합니다.
12. 앱을 실행해 Swagger(`/swagger-ui/index.html`)와 API 응답을 확인합니다.

데이터를 쓰는 요청은 사용자 승인을 받은 뒤 호출합니다.
