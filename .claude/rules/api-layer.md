---
paths:
  - "backend/src/main/java/**/controller/**"
  - "backend/src/main/java/**/dto/**"
---

# API 계층 규칙 (controller, dto)

- 반환 타입은 `ResponseEntity<ApiResponse<T>>`로 통일한다. 본문이 없는 204만 `ResponseEntity<Void>`.
- 성공 응답은 `ResponseUtils.ok(dto)`, 생성은 `ResponseUtils.created(dto)`, 204는 `ResponseUtils.noContent()`만 쓴다. `ResponseEntity.ok(...)`를 직접 만들지 않는다.
- 목록 응답은 `PageResponse.of(page, content)`로 만든다.
- 과제 API는 모두 `/api`로 시작한다. 로그인한 사용자는 `@AuthenticationPrincipal Long userId`로 받는다 (공개 API에서는 null일 수 있다).
- 응답 DTO에 `boolean isXxx` 필드를 두면 Lombok getter `isXxx()`가 JSON에서 `xxx`로 읽힌다. 필드에 `@Getter(AccessLevel.NONE)`을 붙이고 `@JsonProperty("isXxx") public boolean getIsXxx()`를 직접 만든다. getter 이름을 `isXxx()`로 두면 `getXxx()`가 있을 때 그 필드가 응답에서 사라진다. (예: `StudyPostDetailResponse.isAuthor`, `.dev/learnings/003`)
- 응답 DTO의 JSON 모양을 바꾸면 직렬화 테스트(`*JsonTest`)로 필드 이름을 확인한다.
- 응답의 날짜 필드는 `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")`를 붙인다.
- 엔티티를 그대로 반환하지 않는다. 반드시 `dto/`의 Response DTO로 변환한다.
- 에러는 `throw new Backend14thException(ErrorCode.X)`로 던진다. 컨트롤러에서 try-catch로 에러 응답을 만들지 않는다.
- 비즈니스 로직은 service에 둔다. 컨트롤러는 요청 검증, service 호출, 응답 변환만 한다.
- 요청 DTO에는 `jakarta.validation` 어노테이션(`@NotBlank` 등)을 달고, 컨트롤러 파라미터에 `@Valid`를 붙인다.
- Swagger 문서화
  - 클래스: `@Tag(name = "<도메인> API", description = "...")`
  - 메서드: `@Operation(summary = "...", description = "...")` (한글, `~해요` 체)
  - PathVariable/RequestParam: `@Parameter(description = "...", example = "...")`
- URL은 복수형 명사, kebab-case를 쓴다. (예: `/api/study-posts`, `/api/study-posts/{postId}`)
