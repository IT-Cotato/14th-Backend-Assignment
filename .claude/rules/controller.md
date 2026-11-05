---
paths:
  - "src/main/java/**/*Controller.java"
---

# Controller 개발 규칙

## 역할과 의존성

- Controller는 HTTP 요청 수신, 입력 검증 연결, 처리 호출, 응답 구성을 담당한다.
- 업무 로직이나 트랜잭션 처리가 복잡해지면 Service로 분리한다.
- 기존 BookController의 Repository 직접 호출 구조를 별도 목적 없이 일괄 개편하지 않는다.
- 의존성은 생성자로 주입한다.

## 요청과 입력 검증

- 요청 본문이 필요한 API는 요청 DTO를 사용한다.
- 필수 여부·길이·허용 범위는 요구사항에 근거해 정한다.
- 요청 DTO에 Bean Validation을 적용하면 `@Valid` 등으로 검증이 실행되도록 연결한다.
- 업무 규칙 검증을 Controller에 길게 구현하지 않는다.

## 성공 응답

- Entity를 직접 반환하지 않고 응답 DTO로 변환한다.
- 새 API의 성공 응답은 `ApiResponse`와 `ResponseUtils`를 사용한다.
- 조회 성공은 200, 리소스 생성 성공은 201을 기본으로 하며, 다른 응답은 API 의미에 맞게 선택한다.
- 기존 `/hello`의 문자열 응답은 별도 요청 없이 변경하지 않는다.

## 실패 처리

- 업무상 실패는 기존 `ErrorCode`와 `Backend14thException` 체계를 사용한다.
- 공통 오류 응답 변환은 `GlobalExceptionHandler`에 맡긴다.
- 예외를 잡아 무시하거나 실패를 성공 응답으로 반환하지 않는다.
- 입력 검증 등 새로운 실패 유형을 추가하면 전역 예외 처리에서 의도한 HTTP 상태와 오류 응답이 반환되는지 확인한다.

## API 문서와 검증

- `@Tag`, `@Operation`과 필요한 파라미터 설명을 실제 API 동작에 맞게 작성한다.
- API를 추가하거나 변경하면 정상 요청과 주요 실패 사례를 테스트 또는 실제 HTTP 호출로 확인한다.
- 경로·요청·응답을 변경할 때는 기존 API 사용에 미칠 영향을 확인한다.

상세 개발 기준은 `docs/conventions.md`,
현재 구조는 `docs/project-structure.md`를 참고한다.