# 코딩 컨벤션

현재 코드에서 실제로 확인되는 규칙과, 앞으로 코드를 추가할 때 지키면 좋은 권장 규칙을 정리한 문서예요.

> 표기 규칙
> - ✅ **현재 규칙**: 지금 코드에서 실제로 지켜지고 있는 규칙이에요. 새 코드도 이 규칙을 따라요.
> - 💡 **권장 규칙**: 아직 코드에 반영되지 않았지만 앞으로 적용하길 권장하는 규칙이에요. 도입할 때는 기존 코드와의 차이를 함께 정리해요.
> - ⚠️ **현재 예외/불일치**: 규칙에서 벗어나 있거나 파일마다 다른 부분이에요.

---

## 1. 패키지 구조

✅ 현재 규칙

- 기본 패키지: `com.cotato.backend14th`
- 기능 코드는 `domain/<도메인명>/` 아래에 레이어별 하위 패키지로 둬요.
  - `controller`, `dto`, `entity`, `repository`
- 여러 도메인이 함께 쓰는 코드는 `global/` 아래에 둬요.
  - `global/common/entity` — 공통 엔티티 (`BaseEntity`)
  - `global/common/response` — 공통 응답 (`ApiResponse`, `ErrorResponse`, `ResponseUtils`)
  - `global/config` — 설정 (`CorsConfig`, `SecurityConfig`, `SwaggerConfig`)
  - `global/exception` — 예외 (`ErrorCode`, `Backend14thException`, `GlobalExceptionHandler`)
- 패키지명은 모두 소문자 단수형이에요 (`book`, `dto`, `entity`).

⚠️ 현재 예외

- `HelloController`는 루트 패키지(`com.cotato.backend14th`)에 있어요. 1주차 실습 코드라서 그대로 남아 있어요.

💡 권장 규칙

- 서비스 레이어를 도입하면 `domain/<도메인명>/service` 패키지를 추가해요.

---

## 2. 네이밍

✅ 현재 규칙

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 컨트롤러 | `<도메인>Controller` | `BookController` |
| 요청 DTO | `<도메인>Request` | `BookRequest` |
| 응답 DTO | `<도메인>Response` | `BookResponse` |
| 엔티티 | 도메인 이름 그대로 (단수) | `Book` |
| 리포지토리 | `<엔티티>Repository` | `BookRepository` |
| 설정 클래스 | `<대상>Config` | `CorsConfig`, `SecurityConfig`, `SwaggerConfig` |
| 커스텀 예외 | `Backend14thException` 하나 | - |
| ErrorCode 상수 | `<도메인>_<상황>` (UPPER_SNAKE_CASE) | `BOOK_NOT_FOUND`, `COMMON_BAD_REQUEST` |
| ErrorCode 코드값 | `<도메인>-<3자리 번호>` | `BOOK-001`, `COMMON-002` |
| 상수 필드 | `private static final` + UPPER_SNAKE_CASE | `ALLOWED_ORIGINS` |
| 컨트롤러 메서드 | 동사 + 도메인 (camelCase) | `getBook`, `createBook` |
| DB 컬럼 (명시한 경우) | snake_case | `created_at`, `updated_at` |

💡 권장 규칙

- 기능이 늘어나 요청 DTO가 여러 개 필요해지면 `<도메인><동작>Request` 형태(예: `BookCreateRequest`)로 구분하는 것을 고려해요. 현재는 `BookRequest` 하나만 있어요.
- 서비스 레이어를 도입하면 `<도메인>Service`로 이름 붙여요.

---

## 3. API 설계 / URL

✅ 현재 규칙

- 컨트롤러는 `@RestController` + 클래스 레벨 `@RequestMapping("/<복수형 리소스>")`를 써요. (`/books`)
- 단건 리소스는 경로 변수로 식별해요. (`/books/{id}`, `@PathVariable Long id`)
- 생성은 `POST /<리소스>`, 조회는 `GET /<리소스>/{id}`예요.
- 요청 바디는 `@RequestBody`로 DTO를 받아요.
- 엔티티를 그대로 반환하지 않고, 응답 DTO(`BookResponse`)로 변환해서 반환해요.

⚠️ 현재 예외

- `GET /hello`는 리소스 기반 URL 규칙과 공통 응답 규칙을 따르지 않아요.

💡 권장 규칙

- 수정/삭제 API를 추가할 때는 같은 리소스 경로에 `PUT`/`PATCH`/`DELETE`를 사용해요. (CORS 허용 메서드에는 이미 포함되어 있어요.)

---

## 4. API 응답 형식

### 4.1 성공 응답

✅ 현재 규칙

- 컨트롤러 메서드의 반환 타입은 `ResponseEntity<ApiResponse<T>>`예요.
- 응답 생성은 `ResponseUtils`를 사용해요.
  - 조회 등 일반 성공: `ResponseUtils.ok(data)` → HTTP 200
  - 생성 성공: `ResponseUtils.created(data)` → HTTP 201
- `ApiResponse`의 생성자는 `private`이고, 정적 팩토리 메서드(`success`, `created`)로만 만들어요.

응답 예시 (`GET /books/1`):

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": 1,
    "title": "...",
    "author": "..."
  },
  "timestamp": "2026-01-01 12:00:00"
}
```

- `timestamp`는 `Asia/Seoul` 기준 `yyyy-MM-dd HH:mm:ss` 문자열이에요.
- `data`가 `null`이면 `data` 필드 자체가 응답에서 빠져요 (`@JsonInclude(NON_NULL)`).

### 4.2 실패 응답

✅ 현재 규칙

- 실패 응답은 `ErrorResponse` 형식이고, `GlobalExceptionHandler`에서만 만들어요. 컨트롤러에서 직접 `ErrorResponse`를 반환하지 않아요.

응답 예시 (`GET /books/999`, 책이 없을 때):

```json
{
  "success": false,
  "status": 404,
  "code": "BOOK-001",
  "message": "책을 찾을 수 없습니다.",
  "method": "GET",
  "path": "/books/999",
  "timestamp": "2026-01-01 12:00:00"
}
```

💡 권장 규칙

- 본문 없는 성공 응답(예: 삭제)이 필요해지면 새 헬퍼를 임의로 만들기보다 `ResponseUtils`에 메서드를 추가해서 한 곳에서 관리해요.

---

## 5. 예외 처리

✅ 현재 규칙

- 비즈니스 오류는 `throw new Backend14thException(ErrorCode.XXX)`로 던져요.
  ```java
  Book book = bookRepository.findById(id)
          .orElseThrow(() -> new Backend14thException(ErrorCode.BOOK_NOT_FOUND));
  ```
- 새로운 오류는 `ErrorCode` enum에 상수를 추가해요.
  - `HttpStatus`, 코드값(`<도메인>-<번호>`), 메시지 세 가지를 지정해요.
  - 도메인별로 `// COMMON`, `// BOOK` 같은 주석으로 그룹을 나눠요.
  - 번호는 도메인 안에서 `001`부터 순서대로 매겨요.
- `ErrorCode` 메시지는 사용자에게 노출되는 문장이라 `~습니다` 체로 써요. (예: `"책을 찾을 수 없습니다."`)
- `GlobalExceptionHandler`의 처리 방식
  - `Backend14thException` → `ErrorCode`의 HTTP 상태로 응답, `log.warn`
  - 그 외 모든 `Exception` → 500 `COMMON-002`로 응답, `log.error` (스택 트레이스 포함)
- 로그 메시지 형식: `[예외종류] {HTTP메서드} {URI} | ...`

⚠️ 현재 한계 / 주의

- `Backend14thException(ErrorCode, String customMessage)` 생성자가 있지만, `ErrorResponse.of(...)`는 `ErrorCode`의 메시지만 사용해요. 그래서 **커스텀 메시지는 로그에만 남고 응답 본문에는 반영되지 않아요.**
- Spring이 던지는 예외(`MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, 존재하지 않는 경로 등)를 처리하는 핸들러가 없어서 모두 **500**으로 응답돼요.
- `COMMON_BAD_REQUEST`(`COMMON-001`)는 정의되어 있지만 사용하는 곳이 없어요.

💡 권장 규칙

- 입력 검증을 도입할 때 `MethodArgumentNotValidException` 등을 처리하는 핸들러를 `GlobalExceptionHandler`에 추가하고, `COMMON_BAD_REQUEST`로 매핑해요.
- 커스텀 메시지를 응답에도 보여주려면 `ErrorResponse`가 예외 메시지를 받을 수 있도록 함께 수정해요.

---

## 6. 엔티티

✅ 현재 규칙

- 모든 엔티티는 `BaseEntity`를 상속해서 `createdAt`, `updatedAt`을 자동 기록해요. (`@EnableJpaAuditing`이 `Backend14thApplication`에 있어요.)
- PK는 `Long id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`예요.
- 엔티티에는 `@Getter`만 두고 `@Setter`는 쓰지 않아요.
- 엔티티는 컨트롤러 응답으로 직접 내보내지 않고 DTO로 변환해요.

⚠️ 현재 불일치

- `Book`은 `@NoArgsConstructor`, `@AllArgsConstructor`를 모두 `public`으로 열어두고, 생성 시 `new Book(null, title, author)`처럼 `id`에 `null`을 넘겨요.
- `BaseEntity`는 `abstract`가 아닌 일반 클래스예요.
- `Book`의 `title`, `author`에는 `@Column` 제약(`nullable`, `length` 등)이 없어요.

💡 권장 규칙

- 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 제한하고, 생성은 `@Builder`나 정적 팩토리 메서드로 해요. (`id`를 외부에서 넘기지 않도록)
- `BaseEntity`는 단독으로 쓰이지 않으니 `abstract`로 선언하는 것을 고려해요.
- 필수 컬럼에는 `@Column(nullable = false)` 등 제약을 명시해요.
- 값 변경이 필요하면 setter 대신 의도가 드러나는 메서드(예: `update(...)`)를 엔티티에 둬요.

---

## 7. DTO

✅ 현재 규칙

- DTO는 `domain/<도메인>/dto`에 두고, 요청/응답을 분리해요.
- Lombok `@Getter`를 써요. 응답 DTO는 `@AllArgsConstructor`로 생성해요.
- 엔티티 → 응답 DTO 변환은 현재 컨트롤러에서 생성자를 직접 호출해서 해요.
  ```java
  new BookResponse(book.getId(), book.getTitle(), book.getAuthor())
  ```

💡 권장 규칙

- 변환 코드가 중복되므로 `BookResponse.from(Book book)` 같은 정적 팩토리 메서드로 모으는 것을 권장해요.
- 요청 DTO에는 Bean Validation 어노테이션(`@NotBlank` 등)을 붙이고 컨트롤러에서 `@Valid`를 사용해요. (도입 시 5장의 예외 핸들러도 함께 추가)
- Java 21을 쓰고 있으니 불변 DTO는 `record` 사용도 고려할 수 있어요. 도입 여부는 팀/스터디 기준에 맞춰 정해요.

---

## 8. 의존성 주입

⚠️ 현재 불일치

- `BookController`: 생성자를 직접 작성해서 주입해요.
- `SecurityConfig`: `@RequiredArgsConstructor` + `private final` 필드로 주입해요.

✅ 공통점

- 두 방식 모두 **생성자 주입**이고, 필드는 `private final`이에요. 필드 주입(`@Autowired` 필드)은 쓰지 않아요.

💡 권장 규칙

- 새 코드는 `@RequiredArgsConstructor` + `private final` 필드 방식으로 통일해요.

---

## 9. Swagger (springdoc)

✅ 현재 규칙

- 컨트롤러 클래스에 `@Tag(name = "<도메인> API", description = "...")`를 붙여요. (예: `@Tag(name = "Book API", description = "책 관련 API")`)
- 각 엔드포인트에 `@Operation(summary = "...", description = "...")`를 붙여요.
  - `summary`: 짧은 명사형 (예: `"책 단건 조회"`, `"책 등록"`)
  - `description`: `~해요` 체 문장 (예: `"ID로 책 정보를 조회해요."`)
- 경로 변수에는 `@Parameter(description = "...", example = "...")`를 붙여요.
- 전역 문서 정보(title, description, version)는 `SwaggerConfig`에서 관리해요.
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

⚠️ 현재 참고 사항

- `BookController`에 `io.swagger.v3.oas.annotations.responses.ApiResponses` import가 있지만 사용하지 않아요. 프로젝트의 `ApiResponse`(공통 응답)와 이름이 비슷하니 Swagger 어노테이션을 쓸 때 혼동하지 않도록 주의해요.
- `SwaggerConfig`의 title은 `"My API"`예요.

💡 권장 규칙

- 실패 응답(예: 404)도 문서에 드러내고 싶다면 `@ApiResponses`를 사용하되, 공통 응답 클래스와 이름이 겹치므로 패키지명을 포함해서 쓰거나 import를 정리해요.

---

## 10. 설정 / 보안

✅ 현재 규칙

- 환경별 값(DB 접속 정보 등)은 코드에 쓰지 않고 `.env` → 환경 변수 → `application-<profile>.yml`의 `${...}` 플레이스홀더로 주입해요.
- `.env`는 `.gitignore`에 포함되어 있고 **절대 커밋하지 않아요.**
- CORS 허용 Origin/메서드를 바꿀 때는 `CorsConfig`의 `addCorsMappings`와 `corsConfigurationSource()` **두 곳을 모두** 수정해요. Origin 목록은 `ALLOWED_ORIGINS` 상수를 공유해요.
- Security는 stateless이고 CSRF/formLogin/httpBasic이 꺼져 있으며, 현재는 모든 요청을 허용해요.

💡 권장 규칙

- 허용 메서드 목록도 Origin처럼 상수로 빼서 두 설정이 어긋나지 않도록 하는 것을 고려해요.

---

## 11. 코드 스타일 / 주석

✅ 현재 규칙

- 코드 주석과 Swagger 설명은 한국어 `~해요` 체로 써요. (예: `// 세션을 사용하지 않아요`)
- 사용자에게 노출되는 `ErrorCode` 메시지만 `~습니다` 체로 써요.
- Java 소스는 4칸 공백 들여쓰기를 써요.

⚠️ 현재 불일치

- 일부 파일 첫 줄에 `// ApiResponse.java` 같은 파일명 주석이 있고, 없는 파일도 있어요.
- `// 추가`, `// extends BaseEntity 추가` 같은 변경 이력성 주석이 남아 있어요 (`Backend14thApplication`, `Book`).
- import 방식이 섞여 있어요. `BookController`(`org.springframework.web.bind.annotation.*`), `Book`(`jakarta.persistence.*`)은 와일드카드 import를, 나머지는 개별 import를 써요.
- `build.gradle`과 테스트 파일은 탭 들여쓰기를 써요.

💡 권장 규칙

- 파일명 주석과 변경 이력성 주석은 Git 이력으로 대체할 수 있으니 새 코드에는 넣지 않아요.
- import는 개별 import로 통일하는 것을 권장해요.
- 포매터/린터가 없으니, 도입한다면 IDE 코드 스타일 설정이나 포매터 설정 파일을 저장소에 함께 두는 것을 고려해요.

---

## 12. Git / 커밋

✅ 현재 규칙 (CLAUDE.md, `.github/PULL_REQUEST_TEMPLATE.md` 기준)

- 커밋 메시지: `<type>: <한국어 요약>` (예: `feat: 공통 응답 포맷, CORS, Security, BaseEntity 구현`)
  - type: `feat`, `fix`, `chore`, `docs`, `refactor`, `test` 등
- PR 본문은 PR 템플릿을 따르고 **작업 내용**, **관련 이슈(`closes #`)**, **리뷰 포인트**를 포함해요.
- `main`, `develop`에 직접 push하지 않아요.
- 브랜치 전략은 CLAUDE.md의 *Git workflow* 섹션을 따라요.
