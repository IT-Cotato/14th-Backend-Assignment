# 백엔드 컨벤션

## 패키지

- `domain/<도메인>/`에 컨트롤러, DTO, 엔티티, 리포지토리를 둔다. `domain/book/`에서 현재 배치를 확인할 수 있다.
- 공통 응답과 공통 엔티티는 `global/common/`, 예외 처리는 `global/exception/`, 인프라 설정은 `global/config/`에 둔다.
- 테스트는 `src/test/java` 아래에서 대상 코드의 패키지를 따라 배치한다.

## API와 예외

- 새 API의 성공 응답은 `ResponseUtils`를 사용한다. 책 조회는 `ok`로 200을, 등록은 `created`로 201을 반환한다.
- 엔티티에서 필요한 필드만 응답 DTO에 담는다. 책 API의 `BookResponse`가 예시다.
- 도메인 오류는 `ErrorCode`와 `Backend14thException`으로 표현한다. `GlobalExceptionHandler`가 HTTP 상태와 `ErrorResponse`를 만든다.
- `SecurityConfig`는 현재 실습용으로 모든 요청을 허용한다. 인증이나 CORS 정책 변경은 작업 요구사항을 확인한 뒤 별도로 검증한다.
