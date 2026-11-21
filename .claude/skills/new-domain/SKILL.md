---
name: new-domain
description: '새 도메인의 기본 뼈대(Entity, Request/Response DTO, Repository, Controller, ErrorCode)를 프로젝트 컨벤션대로 만든다. 사용자가 새 도메인, 새 엔티티, 새 기능 영역(예: 지원자, 서류, 운영진)을 추가하자고 하거나 "/new-domain 도메인명"으로 부를 때 사용.'
---

# 새 도메인 뼈대 만들기

도메인 이름: $ARGUMENTS

## 1. 먼저 확인하기 (코드 작성 전)
아래 내용을 사용자에게 정리해서 보여주고, 모호하면 질문해요. 승인을 받은 뒤에 2단계로 넘어가요.
- 도메인 이름 (영문 단수형, 예: `applicant`) 과 클래스 이름 (예: `Applicant`)
- 엔티티 필드 목록과 타입 (예: `name: String`, `email: String`)
- 필요한 API. `docs/api-spec.md`에 있으면 그 명세를 그대로 따르고, 없으면 기본값(단건 조회 `GET /api/{도메인 복수형}/{id}`, 생성 `POST /api/{도메인 복수형}`)을 제안해요
- ErrorCode prefix (예: `APPLICANT`)

## 2. 파일 만들기
`backend/src/main/java/com/cotato/backend14th/domain/{도메인}/` 아래에 만들어요.
기존 `domain/book/` 코드를 참고해서 같은 스타일로 써요.

| 파일 | 위치 | 지켜야 할 것 |
|---|---|---|
| `Xxx.java` | `entity/` | `BaseEntity` 상속, `@Id` + `IDENTITY`, `@Getter`, `@NoArgsConstructor`, `@Setter` 금지 |
| `XxxRequest.java` | `dto/` | 클라이언트가 보내는 필드만 |
| `XxxResponse.java` | `dto/` | 클라이언트에게 보여줄 필드만. 엔티티를 그대로 노출하지 않음 |
| `XxxRepository.java` | `repository/` | `JpaRepository<Xxx, Long>` 상속 |
| `XxxService.java` | `service/` | 비즈니스 로직과 권한 검사(작성자 본인인지)를 여기서 해요. 조회는 `@Transactional(readOnly = true)` |
| `XxxController.java` | `controller/` | `.claude/rules/controller.md` 규칙을 따름 (`ResponseUtils`, `@Tag`, `@Operation`, 생성자 주입). Repository를 직접 부르지 않고 Service를 불러요 |
| `XxxServiceTest.java` | `backend/src/test/...` 같은 패키지 | 서비스 핵심 로직의 단위 테스트 (정상 1개 이상, 예외 1개 이상) |

## 3. ErrorCode 추가하기
- `backend/src/main/java/com/cotato/backend14th/global/exception/ErrorCode.java`에 `XXX_NOT_FOUND(HttpStatus.NOT_FOUND, "XXX-001", "...을(를) 찾을 수 없습니다.")`를 추가해요.
- 마지막 상수 뒤의 `;` **앞에** 넣어요. `;` 뒤에 넣으면 컴파일 에러가 나요.
- 같은 prefix가 이미 있으면 다음 번호를 써요.

## 4. 검증하기
- `backend/` 폴더에서 `./gradlew build`를 실행해서 `BUILD SUCCESSFUL`이 나오는지 확인해요.
- 실패하면 에러를 읽고 고친 뒤 다시 빌드해요.

## 5. 결과 보고
- 만든 파일 목록과 추가한 ErrorCode를 정리해서 보여줘요.
- Swagger UI(http://localhost:8080/swagger-ui/index.html)에서 확인할 API를 알려줘요.

## 하지 않는 것
- `global/config/`, `build.gradle`은 건드리지 않아요.
- 1단계 승인 없이 파일을 만들지 않아요.
