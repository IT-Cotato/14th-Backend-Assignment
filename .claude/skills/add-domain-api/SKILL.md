---
name: add-domain-api
description: 이 프로젝트에 새 도메인이나 API를 추가할 때 사용합니다. 패키지 구성, 응답 형식, 에러 코드, Swagger 문서화 순서를 안내합니다.
---

# 도메인 API 추가

`domain/book/`을 기준 예제로 삼아 다음 순서로 진행합니다.

1. `docs/conventions.md`와 `domain/book/`의 코드를 읽습니다.
2. `domain/{도메인}/` 아래에 `controller`, `dto`, `entity`, `repository` 패키지를 만듭니다.
3. 엔티티를 작성합니다. `BaseEntity`를 상속하고 `.claude/rules/entity-changes.md`를 따릅니다.
4. `JpaRepository`를 상속한 repository를 작성합니다.
5. 요청 DTO와 응답 DTO를 작성합니다. 엔티티를 응답으로 직접 반환하지 않습니다.
6. 필요한 에러를 `ErrorCode`에 `{도메인}-{3자리 번호}` 형식으로 추가합니다.
7. 컨트롤러를 작성합니다.
   - 성공 응답은 `ResponseUtils`로 반환합니다.
   - 예외는 `Backend14thException`으로 던집니다.
   - `@Tag`, `@Operation`, `@Parameter`를 붙입니다.
8. 정상과 실패 경우의 테스트를 작성합니다.
9. 실행 전에 테스트가 로컬 DB에 영향을 줄 수 있음을 사용자에게 알리고 테스트를 실행합니다.
10. 앱을 실행해 Swagger(`/swagger-ui/index.html`)와 API 응답을 확인합니다.

데이터를 쓰는 요청은 사용자 승인을 받은 뒤 호출합니다.
