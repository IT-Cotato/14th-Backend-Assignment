# 코드 컨벤션

## 응답 형식

- Controller 반환 타입: `ResponseEntity<ApiResponse<DTO>>`
- 조회 등 일반 성공: `ResponseUtils.ok(data)` → 200
- 생성 성공: `ResponseUtils.created(data)` → 201
- 성공 응답 필드: `success`, `status`, `data`, `timestamp` (`data`가 null이면 필드 생략)
- `timestamp` 형식: `yyyy-MM-dd HH:mm:ss` (Asia/Seoul)

## 에러 처리

- 에러는 `ErrorCode` enum에 `HttpStatus`, 코드, 메시지를 한 세트로 정의
- 상수명: `도메인_상황` (예: `BOOK_NOT_FOUND`, `COMMON_BAD_REQUEST`)
- 코드 형식: `도메인-번호` (예: `BOOK-001`, `COMMON-001`)
- 예외는 `throw new Backend14thException(ErrorCode.XXX)`
- `Backend14thException(ErrorCode.XXX, "메시지")`의 메시지는 예외 메시지(로그)에만 쓰이고, 응답의 `message`에는 `ErrorCode`의 메시지가 나간다
- `GlobalExceptionHandler`(`@RestControllerAdvice`)가 변환
  - `Backend14thException` → 해당 `ErrorCode`의 상태 코드
  - 그 외 `Exception` → 500 (`COMMON-002`)
- 에러 응답 필드: `success`(false), `status`, `code`, `message`, `method`, `path`, `timestamp`

## DTO 네이밍

- 요청: `도메인명 + Request` (예: `BookRequest`)
- 응답: `도메인명 + Response` (예: `BookResponse`)
- 위치: `domain/<도메인>/dto/`
- Request는 `@Getter`, Response는 `@Getter` + `@AllArgsConstructor`

## 엔티티

- `BaseEntity`를 상속해 `createdAt`, `updatedAt`(`created_at`, `updated_at`)을 자동 기록. `@EnableJpaAuditing`은 `Backend14thApplication`에 있다
- PK: `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`인 `Long id`
- 어노테이션: `@Entity` `@Getter` `@NoArgsConstructor` `@AllArgsConstructor`
- `@AllArgsConstructor`는 상속받은 필드를 포함하지 않으므로 `new Book(null, title, author)`처럼 생성한다
