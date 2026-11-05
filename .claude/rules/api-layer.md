---
paths:
  - "src/main/java/**/controller/**"
  - "src/main/java/**/dto/**"
---

# API 계층 규칙 (controller, dto)

- 반환 타입은 `ResponseEntity<ApiResponse<T>>`로 통일한다.
- 성공 응답은 `ResponseUtils.ok(dto)`, 생성은 `ResponseUtils.created(dto)`만 쓴다. `ResponseEntity.ok(...)`를 직접 만들지 않는다.
- 엔티티를 그대로 반환하지 않는다. 반드시 `dto/`의 Response DTO로 변환한다.
- 에러는 `throw new Backend14thException(ErrorCode.X)`로 던진다. 컨트롤러에서 try-catch로 에러 응답을 만들지 않는다.
- 비즈니스 로직은 service에 둔다. 컨트롤러는 요청 검증, service 호출, 응답 변환만 한다.
- 요청 DTO에는 `jakarta.validation` 어노테이션(`@NotBlank` 등)을 달고, 컨트롤러 파라미터에 `@Valid`를 붙인다.
- Swagger 문서화
  - 클래스: `@Tag(name = "<도메인> API", description = "...")`
  - 메서드: `@Operation(summary = "...", description = "...")` (한글, `~해요` 체)
  - PathVariable/RequestParam: `@Parameter(description = "...", example = "...")`
- URL은 복수형 명사, kebab-case를 쓴다. (예: `/books`, `/books/{id}`)
