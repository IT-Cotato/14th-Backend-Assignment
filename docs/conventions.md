# 컨벤션

실제 코드에서 쓰는 규칙을 정리한 문서입니다. "5,6주차부터"라고 적힌 항목은 [API 명세](api-spec.md)를 구현할 때 적용할 규칙이고, 아직 코드에는 없습니다.

## 네이밍

| 대상 | 패턴 | 현재 코드 |
|---|---|---|
| 컨트롤러 | `{도메인}Controller` | `BookController` |
| 서비스 (5,6주차부터) | `{도메인}Service` | 없음 |
| 리포지토리 | `{도메인}Repository` | `BookRepository` |
| 엔티티 | `{도메인}` | `Book` |
| 요청 DTO | `{도메인}Request` | `BookRequest` |
| 응답 DTO | `{도메인}Response` | `BookResponse` |
| 설정 클래스 | `{대상}Config` | `CorsConfig`, `SecurityConfig`, `SwaggerConfig` |
| 에러 코드 상수 | `{도메인}_{상황}` | `BOOK_NOT_FOUND` |
| 에러 코드 값 | `{도메인}-{3자리 번호}` | `BOOK-001`, `COMMON-001` |

5,6주차의 에러 코드 상수 이름과 값은 [API 명세 8장](api-spec.md#8-에러-코드)을 그대로 따릅니다. `INVALID_INPUT`, `UNAUTHORIZED`, `NOT_POST_AUTHOR`처럼 `{도메인}_{상황}` 패턴과 다른 이름도 명세가 우선합니다. 명세에 없는 에러를 새로 만들 때만 위 패턴을 씁니다.

## DTO

- `domain/{도메인}/dto` 패키지에 둡니다.
- `BookRequest`는 Lombok `@Getter`를 사용합니다.
- `BookResponse`는 Lombok `@Getter`, `@AllArgsConstructor`를 사용합니다.
- 컨트롤러에서 엔티티를 응답 DTO로 직접 변환합니다.

```java
new BookResponse(book.getId(), book.getTitle(), book.getAuthor())
```

현재 요청 DTO에는 검증 어노테이션이 없고, 컨트롤러에도 `@Valid`를 쓰지 않습니다.

5,6주차부터는 명세의 "검증" 규칙을 요청 DTO의 검증 어노테이션과 컨트롤러의 `@Valid`로 적용합니다. 검증에 실패하면 `INVALID_INPUT`(400)을 내려줍니다.

## 성공 응답

`BookController`는 `ResponseEntity<ApiResponse<T>>`를 반환하고, `ResponseUtils`로 응답을 만듭니다.

```java
return ResponseUtils.ok(data);       // 200
return ResponseUtils.created(data);  // 201
```

```json
{
  "success": true,
  "status": 200,
  "data": { "id": 1, "title": "제목", "author": "저자" },
  "timestamp": "2026-01-01 12:00:00"
}
```

- `data`가 null이면 응답에서 필드가 빠집니다.
- `HelloController`는 이 형식을 쓰지 않고 `String`을 그대로 반환합니다.
- `timestamp`는 `yyyy-MM-dd HH:mm:ss` 형식입니다. `data` 안의 날짜(`createdAt` 등)는 별도 설정 없이 ISO 형식(`2026-11-08T12:00:00`)으로 나갑니다.
- 5,6주차부터 본문이 없는 204 응답은 감싸지 않습니다. `ResponseEntity<Void>`로 선언하고 `ResponseEntity.noContent().build()`로 반환합니다.

## 예외 처리

`ErrorCode`의 각 항목은 HTTP 상태, 코드, 메시지로 구성됩니다.

```java
BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK-001", "책을 찾을 수 없습니다.")
```

예외는 `Backend14thException`으로 던집니다. 생성자는 두 가지입니다.

```java
new Backend14thException(ErrorCode.BOOK_NOT_FOUND);
new Backend14thException(errorCode, customMessage);
```

`customMessage`는 예외 메시지와 로그에 사용됩니다. 응답의 `message`에는 `ErrorCode`의 메시지가 들어갑니다.

`GlobalExceptionHandler`는 예외를 다음과 같이 처리합니다.

| 예외 | 로그 | 응답 |
|---|---|---|
| `Backend14thException` | `warn` | 해당 `ErrorCode` |
| 그 외 `Exception` | `error` | `COMMON_INTERNAL_SERVER_ERROR` (`COMMON-002`, 500) |

```json
{
  "success": false,
  "status": 404,
  "code": "BOOK-001",
  "message": "책을 찾을 수 없습니다.",
  "method": "GET",
  "path": "/books/999999",
  "timestamp": "2026-01-01 12:00:00"
}
```

`COMMON_BAD_REQUEST`(`COMMON-001`)는 정의되어 있지만 현재 사용하는 코드는 없습니다. 5,6주차에서는 400 요청 값 오류에 `INVALID_INPUT`(`COMMON-003`)만 쓰고, `COMMON_BAD_REQUEST`는 구현할 때 지울 후보입니다.

지금 핸들러는 `@Valid` 검증 실패와 잘못된 쿼리 값을 그 외 `Exception`으로 잡아 500을 내려줍니다. Security 필터에서 나는 401은 `@RestControllerAdvice`까지 오지 않습니다. 5,6주차 구현 때 이 경우들도 명세대로 `ErrorResponse` 형식(400 `INVALID_INPUT`, 401 `UNAUTHORIZED`)으로 내려주도록 처리를 추가해야 합니다.

## 엔티티

- `BaseEntity`를 상속하면 `created_at`, `updated_at`이 JPA Auditing으로 기록됩니다.
- 현재 엔티티는 `Book` 하나이며 `BaseEntity`를 상속합니다.
- `Book`의 ID는 `GenerationType.IDENTITY`로 생성합니다.
- 변경 절차는 [데이터베이스 문서](database.md)를 따릅니다.

## Swagger

`BookController`는 다음 어노테이션을 사용합니다.

- 컨트롤러: `@Tag`
- 메서드: `@Operation`
- 경로 변수: `@Parameter`
