# AGENTS.md

## 프로젝트 개요

- COTATO 14기 스터디 매칭 플랫폼 개발을 위한 풀스택 모노레포 프로젝트이다.
- 프론트엔드는 React, TypeScript, Vite를 사용한다.
- 백엔드는 Java 17, Spring Boot 3.5.16, Gradle Wrapper 8.14.5을 사용한다.
- 백엔드는 MySQL, Spring Data JPA, Spring Security를 사용한다.
- 백엔드 베이스 패키지는 `com.cotato.backend14th`이다.
- 기존 백엔드에는 공통 응답 형식, CORS, Security, BaseEntity, Swagger 관련 코드가 구현되어 있다.

## 프로젝트 구조

- `frontend/`: React + TypeScript 프론트엔드
- `backend/`: Spring Boot REST API 서버
- `docs/`: 프로젝트 개발 문서
- `.agents/skills/`: Codex에서 사용하는 재사용 가능한 작업 절차
- `.codex/`: Codex 설정

## 참고 문서

- 프로젝트 구조 및 아키텍처: `docs/architecture.md`
- API 응답 및 예외 처리 규칙: `docs/api-conventions.md`
- 개발 환경 및 실행 방법: `docs/development-guide.md`

## 주요 명령어

### 백엔드

- 빌드 및 테스트: `cd backend && ./gradlew build`
- 전체 테스트: `cd backend && ./gradlew test`
- 애플리케이션 실행: `cd backend && ./gradlew bootRun`

### 프론트엔드

- 의존성 설치: `cd frontend && npm install`
- 개발 서버 실행: `cd frontend && npm run dev`
- 빌드: `cd frontend && npm run build`
- 린트 검사: `cd frontend && npm run lint`

## 작업 원칙

- 작업 전 기존 코드와 관련 문서를 확인한다.
- 프론트엔드와 백엔드의 역할을 구분한다.
- 기존 코드 구조와 코딩 컨벤션을 우선적으로 따른다.
- 요청받은 작업 범위를 벗어나는 불필요한 변경은 하지 않는다.
- 새로운 의존성을 추가하기 전에 필요성을 검토한다.
- 기능 구현 및 수정 시 관련 테스트를 작성하거나 보완한다.
- 작업 완료 후 관련 테스트 및 빌드를 실행하여 결과를 확인한다.
- 변경 파일, 주요 변경사항, 테스트 결과를 간결하게 보고한다.
- 요구사항이 불명확하거나 중요한 설계 결정이 필요한 경우 먼저 질문한다.

## 안전 규칙

- API 키, 비밀번호 등 민감한 정보를 코드에 하드코딩하거나 Git에 커밋하지 않는다.
- 사용자 승인 없이 데이터베이스의 테이블이나 데이터를 삭제하지 않는다.
- 사용자 승인 없이 운영 환경 설정을 변경하지 않는다.
- 기존 API의 요청 및 응답 형식을 임의로 변경하지 않는다.
- 작업과 무관한 파일을 임의로 수정하거나 삭제하지 않는다.
- 사용자 승인 없이 강제 푸시하거나 Git 히스토리를 변경하지 않는다.
- 테스트 실패를 숨기거나 검증하지 않은 기능을 정상 동작한다고 보고하지 않는다.