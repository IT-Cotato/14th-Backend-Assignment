# Backend Guidelines

## 기술 스택

- Java 17, Spring Boot 3.5.16, Gradle Wrapper 8.14.5
- Spring Data JPA, Spring Security, MySQL
- 베이스 패키지: `com.cotato.backend14th`

## 코딩 규칙

- 기존 코드 구조와 네이밍 방식을 우선적으로 따른다.
- 도메인별 코드는 `domain/{도메인명}/`에 배치한다.
- 공통 기능은 기존 `global/` 패키지 구조를 따른다.
- 중복 코드를 줄이되 불필요한 추상화는 지양한다.
- 새로운 의존성을 추가하기 전에 필요성을 검토한다.

## API 구현

- 기존 공통 응답 구조를 유지한다.
- 성공 응답은 `ResponseUtils` 사용을 우선 고려한다.
- 비즈니스 예외는 `Backend14thException`과 `ErrorCode`를 활용한다.
- API 규칙은 `docs/api-conventions.md`를 참고한다.
- 기존 API의 요청 및 응답 형식을 임의로 변경하지 않는다.

## 테스트 규칙

- JUnit Platform 기반으로 테스트를 작성한다.
- 기존 테스트의 구조와 네이밍 방식을 따른다.
- 테스트는 독립적으로 실행 가능해야 한다.
- 정상 동작과 주요 예외 상황을 함께 검증한다.
- 테스트 실행 순서에 의존하지 않는다.
- 실패한 테스트를 임의로 삭제하거나 비활성화하지 않는다.

## 검증 명령어

- 전체 테스트: `./gradlew test`
- 빌드: `./gradlew build`

위 명령어는 `backend/` 디렉터리에서 실행한다.