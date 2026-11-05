# CLAUDE.md

## 프로젝트 개요

코테이토 14기 백엔드 스터디의 공용 과제 저장소다.
Java 21, Gradle Wrapper, Spring Boot 4.1.1, Spring Data JPA, MySQL을 사용한다.
기본 패키지는 `com.cotato.backend14th`다. 현재 `/hello`, 책 등록·단건 조회 실습 API, 공통 응답과 예외 처리, Swagger 설정이 있다.

## 어디서 무엇을 읽을지

- 패키지 배치와 응답 형식은 [백엔드 컨벤션](docs/backend-conventions.md)을 읽는다.
- DB 연결, Swagger, 로컬 테스트는 [개발 환경 점검](docs/local-verification.md)을 읽는다.
- 저장소 작업에는 [.claude/rules/repository.md](.claude/rules/repository.md)가 적용된다.
- 컨트롤러를 수정할 때는 [.claude/rules/controller.md](.claude/rules/controller.md)가 적용된다.
- 개발 환경을 처음 띄우거나 설정을 바꾼 뒤에는 `/verify-local`을 사용한다.

## 주요 명령어

| 작업 | macOS·Linux·Git Bash | Windows PowerShell |
| --- | --- | --- |
| 컴파일과 패키징 | `./gradlew assemble` | `./gradlew.bat assemble` |
| 테스트 | `./gradlew test` | `./gradlew.bat test` |
| 빌드와 테스트 | `./gradlew build` | `./gradlew.bat build` |
| 로컬 실행 | `./gradlew bootRun` | `./gradlew.bat bootRun` |

테스트와 앱 실행 전 로컬 MySQL 연결을 준비한다. 훅을 실행하려면 `node`가 PATH에 있어야 한다.

## 작업 방식

- 요청 범위와 관련 코드를 확인한 뒤 기존 구현을 재사용한다. 여러 파일에 걸친 변경은 먼저 작업 순서와 검증 방법을 정한다.
- 기능을 바꿀 때는 성공과 실패 응답을 함께 확인한다. 완료 보고에는 실제 실행한 명령과 결과를 적는다.
- 검증이 막히면 실패한 단계와 원인을 남긴다.

## 영구 금지

- 실제 `.env`, DB 자격 증명, 토큰을 커밋하거나 출력하지 않는다.
- 공용 `develop`에 직접 push하거나 강제 push하지 않는다.
- 공유 DB나 운영 DB에서 로컬 검증을 실행하지 않는다.
