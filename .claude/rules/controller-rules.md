---
paths:
  - "src/main/java/**/controller/**"
---

# Controller 작성 규칙

> **적용 범위**: `src/main/java/**/controller/**`에 있는 파일을 읽거나 수정할 때만 적용돼요. 현재 해당 파일은 `domain/book/controller/BookController.java` 하나예요.
> 루트 패키지의 `HelloController.java`는 `controller/` 경로 밖에 있어서 이 규칙이 적용되지 않아요.
> **목적**: 새 Controller나 엔드포인트를 추가할 때 지금 프로젝트의 응답·예외 규칙과 실제 레이어 구조(Service 없음)를 그대로 따르도록 해요.
>
> 규칙의 근거는 `docs/coding-conventions.md`(3~5장, 9장)와 `docs/project-structure.md`예요. 여기에는 Controller를 작성할 때 바로 지켜야 할 내용만 추렸어요.

---

## 1. 위치와 클래스 구성

**목적**: 도메인별 패키지 구조를 유지해요.

- Controller는 `com.cotato.backend14th.domain.<도메인>.controller` 패키지에 `<도메인>Controller` 이름으로 둬요.
- 클래스에는 `@Tag`, `@RestController`, `@RequestMapping("/<복수형 리소스>")`를 붙여요. (`BookController`: `@RequestMapping("/books")`)
- 의존성은 `private final` 필드 + 생성자 주입으로 받아요. 필드 주입(`@Autowired` 필드)은 쓰지 않아요.
  - 새 Controller는 `@RequiredArgsConstructor`를 써요. (`docs/coding-conventions.md` 8장 권장 규칙)
  - 기존 `BookController`의 직접 작성한 생성자는 요청 없이 바꾸지 않아요.

## 2. Service 계층은 아직 없어요

**목적**: 존재하지 않는 클래스를 가정해서 컴파일이 깨지거나, 요청하지 않은 구조 변경이 들어가는 걸 막아요.

- 현재 Controller는 `<도메인>Repository`를 **직접 주입받아 호출**해요. `*Service` 클래스와 `service` 패키지는 프로젝트에 없어요.
- 새 엔드포인트도 이 구조를 따라 Repository를 직접 호출해요. `XxxService`를 import·주입하거나 있는 것처럼 코드를 쓰지 않아요.
- Service 계층 도입은 별도 작업이에요. 사용자가 명시적으로 요청했을 때만 하고, 그때는 영향받는 파일과 구조 변경 범위를 먼저 설명해요.
- 지금 구조에서는 조회, 저장, 엔티티 → DTO 변환이 Controller 메서드 안에 있어요. `@Transactional`도 쓰지 않아요. 여러 Repository 호출을 하나의 트랜잭션으로 묶어야 하는 기능이 필요하면 Controller에서 억지로 처리하지 말고 사용자에게 Service 도입 여부를 물어봐요.

## 3. 성공 응답: `ResponseUtils` + `ApiResponse`

**목적**: 모든 성공 응답이 `success / status / data / timestamp` 형식으로 같도록 해요.

- 반환 타입은 항상 `ResponseEntity<ApiResponse<T>>`예요.
- 응답은 `ResponseUtils`로만 만들어요.
  - 조회 등 일반 성공 → `ResponseUtils.ok(data)` (200)
  - 리소스 생성 → `ResponseUtils.created(data)` (201)
- `new ResponseEntity<>(...)`, `ResponseEntity.ok(...)`, `ApiResponse.success(...)`를 Controller에서 직접 호출하지 않아요. `ApiResponse` 생성자는 `private`이에요.
- `data`에는 **응답 DTO**(`<도메인>Response`)를 넣고 엔티티를 그대로 넣지 않아요.
- `ResponseUtils`에 없는 응답(예: 본문 없는 204)이 필요하면 Controller에서 따로 만들지 말고, `global/common/response`를 수정해야 한다고 사용자에게 알려요. (Controller 범위 밖의 변경이에요.)

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<BookResponse>> getBook(@PathVariable Long id) {
    Book book = bookRepository.findById(id)
            .orElseThrow(() -> new Backend14thException(ErrorCode.BOOK_NOT_FOUND));
    return ResponseUtils.ok(new BookResponse(book.getId(), book.getTitle(), book.getAuthor()));
}
```

## 4. 실패 처리: `Backend14thException` + `ErrorCode`

**목적**: 실패 응답을 `GlobalExceptionHandler` 한 곳에서 `ErrorResponse` 형식으로 만들도록 해요.

- 실패는 `throw new Backend14thException(ErrorCode.XXX)`로 던져요. Controller에서 `ErrorResponse`를 직접 만들거나 반환하지 않아요.
- Controller에서 `try-catch`로 예외를 잡아 응답을 만들지 않아요. `@ExceptionHandler`도 Controller 안에 두지 않아요.
- 맞는 `ErrorCode`가 없으면 `ErrorCode` enum에 새 상수를 추가해요.
  - 이름 `<도메인>_<상황>`, 코드 `<도메인>-<3자리 번호>`(도메인 안에서 다음 번호), 메시지는 `~습니다` 체
  - 해당 도메인 주석 그룹 아래에 추가해요.
- `Backend14thException(errorCode, customMessage)`의 커스텀 메시지는 **응답 본문에 나가지 않고 로그에만 남아요.** 사용자에게 다른 메시지를 보여줘야 하면 별도 `ErrorCode`를 만들어요.

## 5. 요청 입력 검증

**목적**: 검증을 붙였을 때 400이 아니라 500이 나가는 상황을 막아요.

- 현재 어떤 Controller도 `@Valid`를 쓰지 않아요.
- `GlobalExceptionHandler`에 `MethodArgumentNotValidException` 등 Spring 예외 핸들러가 없어서, `@Valid`만 추가하면 검증 실패가 **500 `COMMON-002`** 로 응답돼요.
- 그래서 `@Valid`를 추가하는 작업은 `GlobalExceptionHandler`에 `COMMON_BAD_REQUEST`로 매핑하는 핸들러를 추가하는 작업과 **함께** 해요. 사용자가 검증을 요청하지 않았다면 임의로 추가하지 않아요.

## 6. Swagger 문서화

**목적**: 모든 엔드포인트가 Swagger UI에 일관된 설명으로 노출되도록 해요.

- 클래스: `@Tag(name = "<도메인> API", description = "...")`
- 메서드: `@Operation(summary = "<짧은 명사형>", description = "<~해요 체 문장>")`
- 경로 변수: `@Parameter(description = "...", example = "...")`
- Swagger의 `io.swagger.v3.oas.annotations.responses.ApiResponse(s)`와 공통 응답 `global.common.response.ApiResponse`는 이름이 겹쳐요. 둘을 같은 파일에서 쓸 때는 Swagger 쪽을 전체 패키지명으로 써서 import 충돌을 피해요.

## 7. 변경 범위

**목적**: Controller 작업 중 다른 레이어나 기존 코드가 몰래 바뀌지 않도록 해요.

- 요청받은 엔드포인트만 추가·수정해요. 기존 엔드포인트의 경로, 응답 형식, 생성자 방식, import 스타일을 겸사겸사 정리하지 않아요.
- Controller 작업 때문에 DTO, `ErrorCode`, Entity, Repository를 바꿔야 하면 어떤 파일을 왜 바꾸는지 함께 알려요.
- `HelloController`를 `ApiResponse` 형식으로 바꾸거나 옮기는 작업은 요청이 있을 때만 해요.

## 작성 후 점검

- [ ] 반환 타입이 `ResponseEntity<ApiResponse<T>>`이고 `ResponseUtils.ok/created`를 썼나요?
- [ ] 엔티티가 아니라 응답 DTO를 반환하나요?
- [ ] 실패는 `Backend14thException(ErrorCode.XXX)`로 던지나요?
- [ ] 없는 Service 클래스를 참조하지 않았나요?
- [ ] `@Tag` / `@Operation` / `@Parameter`를 붙였나요?
- [ ] `@Valid`를 추가했다면 예외 핸들러도 함께 추가했나요?
