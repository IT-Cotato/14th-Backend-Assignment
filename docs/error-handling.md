# 예외 처리 가이드

## 구조

    예외 발생
      → Backend14thException(ErrorCode) 던지기
      → GlobalExceptionHandler가 잡음
      → ErrorResponse로 변환해서 응답

관련 파일은 모두 `global/exception/`과 `global/common/response/`에 있다.

| 파일 | 역할 |
| --- | --- |
| `ErrorCode` | HTTP 상태, 에러 코드, 메시지를 묶어둔 enum |
| `Backend14thException` | `ErrorCode`를 담는 커스텀 `RuntimeException` |
| `GlobalExceptionHandler` | `@RestControllerAdvice`로 예외를 잡아 응답으로 변환 |
| `ErrorResponse` | 클라이언트에게 내려가는 에러 응답 형식 |

## 새 에러 추가하기

1. `ErrorCode` enum에 상수를 추가한다.

       BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK-001", "책을 찾을 수 없습니다.");

2. 코드 문자열은 `도메인-번호` 형식으로 쓴다. 공통 에러는 `COMMON-번호`.
3. 번호는 해당 도메인 안에서 001부터 순서대로 붙인다.

## 예외 던지기

Controller나 Service에서 직접 `RuntimeException`을 던지지 않는다.

    Book book = bookRepository.findById(id)
            .orElseThrow(() -> new Backend14thException(ErrorCode.BOOK_NOT_FOUND));

`RuntimeException`을 그냥 던지면 500으로 내려간다.
`Backend14thException`을 써야 `ErrorCode`에 정의한 상태 코드로 응답된다.

## 주의

- Controller에서 try-catch로 직접 처리하지 않는다. `GlobalExceptionHandler`에 맡긴다.
- Swagger의 `@ApiResponse`에 적은 상태 코드와 실제 `ErrorCode`의 상태 코드를 일치시킨다.