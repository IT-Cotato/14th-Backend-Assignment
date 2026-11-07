# CLAUDE.md

## 프로젝트 개요

Java 21과 Spring Boot 기반의 백엔드 프로젝트다.
Gradle, Spring Data JPA, MySQL, Spring Security, SpringDoc, Lombok을 사용한다.
기본 패키지는 `com.cotato.backend14th`다.
현재 책 등록·단건 조회 API와 `/hello` API가 구현되어 있다.
설명과 문서는 한국어로 작성하고, 코드 식별자와 명령어는 원문을 유지한다.

## 어디서 무엇을 읽을지

작업에 필요한 문서를 먼저 확인한다.

| 상황 | 참고 파일 |
|---|---|
| 현재 구조와 구성요소의 역할 확인 | `docs/project-structure.md` |
| 개발·환경설정·검증·협업 기준 확인 | `docs/conventions.md` |
| 브랜치·커밋·PR 작업 | `.claude/rules/git-workflow.md` |
| Controller 개발·수정 | `.claude/rules/controller.md` |

Git Rule은 항상 적용하고, Controller Rule은 해당 파일의 `paths` 조건에 따라 적용한다.
문서와 코드가 다르면 차이를 알리고 현재 상태와 변경 목표를 구분한다.

## 주요 명령어

프로젝트 루트의 Windows PowerShell에서 Gradle Wrapper를 사용한다.

```powershell
.\gradlew.bat build     # 빌드 및 테스트
.\gradlew.bat test      # 테스트만 실행
.\gradlew.bat bootRun   # 애플리케이션 실행
```

- `build`에 테스트가 포함되므로, 같은 변경에 대해 이유 없이 `test`를 반복하지 않는다.
- Spring 컨텍스트를 불러오는 테스트와 로컬 실행에는 DB 연결 및 환경설정이 필요할 수 있다.
- macOS·Linux에서는 `.\gradlew.bat` 대신 `./gradlew`를 사용한다.

## 로컬 실행 환경

- 기본 프로필은 `local`이며 `SPRING_PROFILES_ACTIVE`로 변경할 수 있다.
- `application.yml`에서 프로젝트 루트의 `.env`를 선택적으로 불러온다.
- 로컬 프로필은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 참조한다.
- `DB_URL`은 기본값이 없고, 사용자명·비밀번호의 기본값은 각각 `root`와 빈 문자열이다.
- 실제 접속 환경에 맞는 값은 사용자가 로컬에서 관리한다.
- 로컬의 `ddl-auto: update` 설정을 운영 환경에 그대로 적용하지 않는다.

## 작업 방식

- 요청한 목표와 완료 조건을 확인하고 필요한 파일부터 읽는다.
- 여러 파일이나 구조를 바꾸는 작업은 변경 범위와 검증 방법을 먼저 설명한다.
- 결과에 영향을 주는 요구사항이 모호하면 질문한다.
- 기존 패턴을 따르고, 작업과 관계없는 리팩터링이나 의존성 변경은 피한다.
- 현재 Book API에는 Service가 없다. 새로운 업무 로직의 복잡도에 맞게 분리를 검토한다.
- 변경 후에는 적절한 검증을 수행하고 변경 내용·이유·검증 결과를 설명한다.
- 빌드 성공, 서버 시작, DB 연결, Swagger 접속, 개별 API 동작을 구분한다.
- 확인하지 못한 항목은 미확인으로 기록하고 이유를 설명한다.
- 구조나 개발 기준이 바뀌면 관련 docs와 Rules도 함께 검토한다.

## 영구 금지

- `.env`와 실제 비밀번호·토큰을 커밋하거나 문서·응답·로그에 노출하지 않는다.
- 문제 해결을 위해 `.env` 전체를 출력하거나 사용자에게 공유하도록 요구하지 않는다.
- `main`과 `develop`에 직접 push하지 않는다.
- 강제 push를 사용하지 않는다.
- 사용자의 기존 변경사항을 임의로 삭제하거나 되돌리지 않는다.
- 요청받지 않은 커밋·push·PR 생성·머지를 수행하지 않는다.
- 실행하지 않은 검증을 통과했다고 보고하지 않는다.