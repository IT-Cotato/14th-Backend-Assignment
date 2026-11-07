---
paths:
  - "src/main/java/**/controller/**"
---

# 컨트롤러 규칙

## 응답
- 반환 타입은 `ResponseEntity<ApiResponse<T>>`로 통일한다.
- 조회/수정은 `ResponseUtils.ok(data)`, 생성은 `ResponseUtils.created(data)`(201)를 쓴다.
- Entity를 그대로 반환하지 않고 `XxxResponse` DTO로 변환한다.

## 구조
- Controller는 요청을 받고 응답을 돌려주는 일만 한다. 로직은 Service에 둔다.
- Repository를 직접 주입받지 않는다. (`BookController`는 연습용 예외)
- 의존성은 `private final` + `@RequiredArgsConstructor` 생성자 주입으로 받는다.
- 클래스에 `@RequestMapping("/복수형-리소스")`를 붙인다. (예: `/books`, `/applicants`)

## 예외
- 실패 상황은 `throw new Backend14thException(ErrorCode.XXX)`로 처리한다.
- 컨트롤러 안에서 try-catch로 에러 응답을 직접 만들지 않는다. `GlobalExceptionHandler`에 맡긴다.

## 요청 검증
- `@RequestBody` DTO에는 `@Valid`를 붙이고, DTO 필드에 `@NotBlank` 같은 검증 어노테이션을 단다.

## Swagger 문서화
- 클래스: `@Tag(name = "Xxx API", description = "...")`
- 메서드: `@Operation(summary = "짧은 제목", description = "설명")`
- 경로변수/쿼리파라미터: `@Parameter(description = "...", example = "1")`
- 실패할 수 있는 API는 `@ApiResponses`로 에러 응답 코드를 적는다.
    - Swagger의 `@ApiResponse`와 우리 `global.common.response.ApiResponse`는 이름이 같다. 둘 다 쓰는 파일에서는 한쪽을 전체 패키지명으로 쓴다.
- DTO 필드에는 `@Schema(description = "...", example = "...")`를 단다.
