---
name: verify-local
description: "Gradle 빌드, 로컬 MySQL 연결, Swagger 응답을 점검한다. 처음 개발 환경을 구성했거나 의존성·DB·API 설정을 변경한 뒤 제출 전에 사용한다."
---

# 로컬 백엔드 점검

1. [로컬 개발 환경 점검](../../../docs/local-verification.md)을 읽고 Java와 Gradle Wrapper를 확인한다.
2. `git check-ignore .env`를 실행한다. DB 설정이 없거나 검증 전용 DB인지 확인할 수 없으면 `assemble`까지만 실행하고 DB와 Swagger 점검을 보류한다.
3. 검증 전용 DB가 준비되면 문서에 맞는 Wrapper 명령으로 `build`를 실행한다. 실패하면 최초 실패 태스크와 원인을 기록한다.
4. 로컬 앱을 실행해 DB 연결 로그, Swagger UI, OpenAPI JSON을 확인한다. HTTP 포트가 사용 중이면 빈 로컬 포트를 선택하고 실제 포트를 기록한다.
5. 이번 점검에서 실행한 앱을 종료하고 빌드, DB, Swagger별로 실행 명령과 결과를 정리한다.
