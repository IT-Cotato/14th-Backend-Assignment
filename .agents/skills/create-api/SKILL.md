---
name: create-api
description: Spring Boot 백엔드에서 새로운 REST API를 구현할 때 사용하는 스킬이다. API 추가, 엔드포인트 구현, 컨트롤러 작성 요청에 활용한다.
---

# REST API 구현 스킬

## 적용 범위

- 이 스킬은 `backend/`의 Spring Boot REST API 구현에 사용한다.
- 작업 시 루트 `AGENTS.md`와 `backend/AGENTS.md`의 지침을 따른다.

## 절차

1. 프로젝트 구조를 확인한다.
   - `docs/architecture.md`를 읽는다.
   - `backend/`의 기존 도메인 Controller, DTO, Entity, Repository를 확인한다.

2. API 명세와 규칙을 확인한다.
   - `docs/api-spec.md`에서 구현할 API의 HTTP Method, 엔드포인트, 요청 및 응답 형식을 확인한다.
   - `docs/api-conventions.md`를 읽는다.
   - 기존 API의 공통 응답 형식과 예외 처리 방식을 파악한다.
   - 명세에 없는 API나 기능을 임의로 추가하지 않는다.
   - 명세와 기존 코드가 충돌하면 임의로 결정하지 않고 사용자에게 확인한다.

3. API 구현 계획을 수립한다.
   - `docs/api-spec.md`에 정의된 요구사항을 기준으로 구현 범위를 확인한다.
   - 필요한 Entity, Repository, DTO, Service, Controller를 식별한다.
   - 인증 및 작성자 권한 검사를 확인한다.
   - 정상 응답과 예외 상황을 정리한다.
   - 기존 구현을 재사용할 수 있는지 확인한다.

4. API를 구현한다.
   - 기존 패키지 구조를 따른다.
   - 공통 응답은 `ResponseUtils`를 활용한다.
   - 비즈니스 예외는 `Backend14thException`과 `ErrorCode`를 활용한다.

5. 테스트를 진행한다.
   - 정상 동작과 주요 예외 상황을 검증한다.
   - 프로젝트 루트에서 `cd backend && ./gradlew test`를 실행한다.
   - 필요하면 `cd backend && ./gradlew build`를 실행한다.

6. 작업 결과를 보고한다.
   - 추가 및 수정한 파일을 정리한다.
   - 구현한 API의 경로와 동작을 설명한다.
   - 테스트 결과와 남은 문제를 보고한다.

## 하지 말 것

- 기존 API의 응답 형식을 임의로 변경하지 않는다.
- 요청받지 않은 기능을 추가하지 않는다.
- 기존 코드 구조를 무시하고 새로운 계층을 무분별하게 만들지 않는다.
- 테스트 실패를 숨기거나 실패한 테스트를 임의로 삭제하지 않는다.
- 사용자 승인 없이 데이터베이스의 데이터를 삭제하지 않는다.