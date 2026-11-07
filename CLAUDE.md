# 프로젝트 안내

## 프로젝트 개요
- 코테이토 14기 백엔드 과제용 Spring Boot 프로젝트다.
- Java 21, Gradle Wrapper, Spring MVC, Spring Data JPA, MySQL을 사용한다.
- 기본 패키지는 `com.cotato.backend14th`다.
- 현재 Hello API와 도서 등록·조회 API, 공통 응답·예외 처리, Swagger가 있다.

## 어디서 무엇을 읽을지
- 프로젝트 구조와 API 흐름: `docs/project-structure.md`
- 실행 환경과 검증: `docs/development.md`
- 브랜치와 제출 규칙: `.claude/rules/workflow.md`
- Java API 수정: `.claude/rules/api.md`
- 제출 전 점검: `.claude/skills/check-assignment/SKILL.md`

## 주요 명령어
저장소 루트에서 Java 21로 실행한다. Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 쓴다.

```sh
./gradlew test
./gradlew bootJar
./gradlew bootRun
python -m unittest discover -s .claude/hooks/tests
```

## 작업 방식
- 구현 전에 현재 코드와 해당 작업의 문서를 확인한다.
- 요청받은 과제 범위 안에서 수정하고, 기존 공통 응답과 예외 처리를 재사용한다.
- 검증은 실행한 명령과 결과를 함께 남긴다. 확인하지 못한 항목은 구분한다.
- 문서가 코드와 다르면 실제 구현을 확인한 뒤 함께 수정한다.

## 영구 금지
- `.env`와 비밀번호, 토큰을 읽거나 출력하거나 커밋하지 않는다.
- `develop`과 개인 누적 브랜치에 작업 내용을 직접 push하지 않는다. 개인 누적 브랜치 최초 생성은 예외다.
- 강제 push, 승인 없는 DB 초기화, 다른 사람 브랜치 수정은 하지 않는다.
- 파트장 검토 전에 PR을 직접 병합하지 않는다.
