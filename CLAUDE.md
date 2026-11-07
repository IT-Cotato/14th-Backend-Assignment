# CLAUDE.md

## 프로젝트 개요

- COTATO 14기 백엔드 과제를 위한 Spring Boot 기반 REST API 프로젝트
- Java 21, Spring Boot 4.1.1, Gradle을 사용
- MySQL과 Spring Data JPA를 사용하며, Spring Security와 Swagger UI가 적용되어 있다.
- 베이스 패키지는 com.cotato.backend14th이다.
- 현재 공통 응답 형식, CORS, Security, BaseEntity 및 Swagger 관련 코드가 반영되어 있으며, Gradle 빌드와 애플리케이션 실행이 성공한 상태이다.

## 어디서 무엇을 읽을지

- 프로젝트 구조와 패키지 역할을 파악할 때 → `docs/architecture.md`
- API 응답 형식과 예외 처리 규칙을 확인할 때 → `docs/api-conventions.md`
- 개발 환경 설정, 빌드 및 테스트 방법을 확인할 때 → `docs/development-guide.md`
- 코딩 및 테스트 규칙을 확인할 때 → `.claude/rules/`의 관련 규칙
- 반복적인 개발 작업을 수행할 때 → `.claude/skills/`의 관련 스킬

## 주요 명령어

- 프로젝트 빌드 및 테스트: `./gradlew build`
- 전체 테스트 실행: `./gradlew test`
- Spring Boot 애플리케이션 실행: `./gradlew bootRun`
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 작업 방식

- 작업 전 기존 코드와 관련 문서를 확인하여 프로젝트 구조와 구현 방식을 파악한다.
- 기존 패키지 구조와 코딩 컨벤션을 유지한다.
- 요청된 작업 범위를 벗어나는 불필요한 수정은 하지 않는다.
- 기능 구현 및 수정 시 관련 테스트를 작성하거나 보완한다.
- 작업 완료 후 관련 테스트와 빌드를 실행하여 정상 동작을 확인한다.
- 변경한 파일과 주요 변경사항, 테스트 결과를 간결하게 보고한다.
- 요구사항이 불명확하거나 중요한 설계 결정이 필요한 경우 임의로 판단하지 않고 먼저 질문한다.

## 영구 금지

- API 키, 비밀번호, 환경 변수 등 민감한 정보를 코드에 하드코딩하거나 Git에 커밋하지 않는다.
- 사용자 승인 없이 데이터베이스의 테이블이나 데이터를 삭제하지 않는다.
- 사용자 승인 없이 운영 환경 설정을 변경하지 않는다.
- 기존 API의 요청 및 응답 형식을 임의로 변경하지 않는다.
- 작업과 무관한 파일을 임의로 수정하거나 삭제하지 않는다.
- 사용자 승인 없이 강제 푸시하거나 Git 히스토리를 변경하지 않는다.
- 테스트 실패를 숨기거나 검증하지 않은 기능을 정상 동작한다고 보고하지 않는다.