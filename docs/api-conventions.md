# API 컨벤션

## 1. 개요

본 프로젝트는 API 응답의 일관성을 유지하기 위해 공통 성공 응답과 오류 응답 구조를 사용한다.

관련 구현 파일:
- `global/common/response/ApiResponse.java`
- `global/common/response/ErrorResponse.java`
- `global/common/response/ResponseUtils.java`
- `global/exception/ErrorCode.java`
- `global/exception/Backend14thException.java`
- `global/exception/GlobalExceptionHandler.java`

## 2. 성공 응답 규칙

성공 응답은 `ApiResponse<T>`를 사용한다.

### 2.1 응답 구조

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": 1,
    "title": "클린 코드"
  },
  "timestamp": "2026-11-03 23:30:00"
}
```

위 JSON의 `data`는 응답 형식을 설명하기 위한 예시이다.

| 필드 | 타입 | 설명 |
|---|---|---|
| success | boolean | 성공 여부 (`true`) |
| status | int | HTTP 상태 코드 |
| data | T | 응답 데이터 |
| timestamp | LocalDateTime | 응답 생성 시각 |

- `data`가 `null`이면 `@JsonInclude(NON_NULL)`에 의해 응답에서 생략된다.
- `timestamp`는 `yyyy-MM-dd HH:mm:ss` 형식으로 직렬화한다.
- 타임스탬프 직렬화에는 `Asia/Seoul` 시간대를 지정한다.

### 2.2 성공 응답 생성

`ResponseUtils`에서 제공하는 메서드를 사용한다.

| 메서드 | HTTP 상태 | 설명 |
|---|---|---|
| `ResponseUtils.ok(data)` | 200 OK | 일반적인 요청 성공 |
| `ResponseUtils.created(data)` | 201 Created | 리소스 생성 성공 |

사용 예시:

```java
return ResponseUtils.ok(data);
```

```java
return ResponseUtils.created(data);
```

새로운 API를 구현할 때 기존 공통 응답 형식을 우선 사용한다.

## 3. 오류 응답 규칙

오류 응답은 `ErrorResponse`를 사용한다.

### 3.1 응답 구조

```json
{
  "success": false,
  "status": 404,
  "code": "BOOK-001",
  "message": "책을 찾을 수 없습니다.",
  "method": "GET",
  "path": "/books/1",
  "timestamp": "2026-11-03 23:30:00"
}
```

위 JSON의 요청 경로와 타임스탬프는 예시이다.

| 필드 | 타입 | 설명 |
|---|---|---|
| success | boolean | 항상 `false` |
| status | int | HTTP 상태 코드 |
| code | String | 프로젝트 오류 코드 |
| message | String | 오류 메시지 |
| method | String | 요청 HTTP 메서드 |
| path | String | 요청 URI |
| timestamp | LocalDateTime | 응답 생성 시각 |

- 오류 응답은 `ErrorResponse.of(errorCode, request)`로 생성한다.
- `method`는 `HttpServletRequest.getMethod()`에서 가져온다.
- `path`는 `HttpServletRequest.getRequestURI()`에서 가져온다.
- 타임스탬프는 `yyyy-MM-dd HH:mm:ss` 형식으로 직렬화하며 `Asia/Seoul` 시간대를 지정한다.

## 4. 오류 코드 관리

오류 코드는 `ErrorCode` enum에서 관리한다.

각 오류 코드는 다음 정보를 포함한다.

- `httpStatus`: HTTP 상태 코드
- `code`: 프로젝트 내부 오류 식별자
- `message`: 기본 오류 메시지

### 4.1 현재 정의된 오류 코드

| 오류 코드 | HTTP 상태 | 메시지 |
|---|---|---|
| COMMON-001 | 400 Bad Request | 잘못된 요청입니다. |
| COMMON-002 | 500 Internal Server Error | 서버 내부 오류가 발생했습니다. |
| BOOK-001 | 404 Not Found | 책을 찾을 수 없습니다. |

### 4.2 오류 코드 추가 규칙

- 새로운 오류 코드는 `ErrorCode` enum에 추가한다.
- 기존의 도메인별 그룹 구성을 따른다.
- 오류 코드는 기존의 `도메인-번호` 형식을 참고한다.
- 기존 오류 코드의 의미를 임의로 변경하지 않는다.

## 5. 예외 처리 규칙

예외 처리는 `@RestControllerAdvice`가 적용된 `GlobalExceptionHandler`에서 담당한다.

### 5.1 사용자 정의 예외

비즈니스 오류를 표현할 때 `Backend14thException`을 사용한다.

```java
throw new Backend14thException(ErrorCode.BOOK_NOT_FOUND);
```

`GlobalExceptionHandler`는 `Backend14thException`을 처리할 때 다음 작업을 수행한다.

1. 예외에 포함된 `ErrorCode`를 확인한다.
2. 요청 메서드, URI, 오류 코드 및 메시지를 WARN 레벨로 기록한다.
3. `ErrorCode`에 정의된 HTTP 상태 코드를 사용한다.
4. `ErrorResponse.of()`를 통해 공통 오류 응답을 반환한다.

### 5.2 처리되지 않은 예외

별도로 처리하지 않은 예외는 `Exception.class` 핸들러에서 처리한다.

- HTTP 상태: `500 Internal Server Error`
- 오류 코드: `COMMON-002`
- 오류 메시지: `서버 내부 오류가 발생했습니다.`
- 로그 레벨: `ERROR`

예상하지 못한 예외의 상세 내용은 로그에 기록하고, 클라이언트에는 공통 서버 오류 메시지를 반환한다.

### 5.3 사용자 정의 메시지 주의 사항

`Backend14thException`은 사용자 정의 메시지를 전달받을 수 있다.

```java
throw new Backend14thException(
    ErrorCode.BOOK_NOT_FOUND,
    "요청한 책이 존재하지 않습니다."
);
```

하지만 현재 `ErrorResponse.of()`는 `ErrorCode`의 기본 메시지를 사용한다.

따라서 사용자 정의 메시지는 예외 메시지 및 로그에 반영될 수 있지만, 클라이언트 응답에는 기본 오류 메시지가 반환된다.

## 6. 신규 API 개발 시 준수 사항

- 성공 응답은 `ApiResponse<T>` 구조를 따른다.
- 성공 응답 생성 시 `ResponseUtils` 사용을 우선 고려한다.
- 비즈니스 오류는 `Backend14thException`과 `ErrorCode`를 활용한다.
- 새로운 오류 코드는 기존 명명 규칙에 맞춰 추가한다.
- 기존 공통 응답 형식과 전역 예외 처리 방식을 임의로 변경하지 않는다.
- API 구현 후 관련 테스트를 실행하여 HTTP 상태 코드와 응답 구조를 확인한다.