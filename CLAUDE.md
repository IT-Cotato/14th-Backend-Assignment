# CLAUDE.md

## 프로젝트 개요
- COTATO 14기 백엔드 과제 레포. Spring Boot 4.1.1 / Java 21 / Gradle
- 주요 의존성: Spring Web MVC, Spring Data JPA + MySQL, Spring Security, Validation, springdoc-openapi, Lombok
- 베이스 패키지: `com.cotato.backend14th` (`global/` 공통, `domain/<도메인>/` 기능별)
- 현재 상태: 공통 응답 포맷, 전역 예외 처리, CORS, Security(전부 permitAll), BaseEntity, Swagger까지 구축. 예제 도메인은 `book`
- DB 접속 정보는 `.env`에서 읽는다 (`application.yml`의 `spring.config.import`)

## 어디서 무엇을 읽을지
- 패키지 구조, 요청 -> 응답/예외 흐름이 궁금할 때 -> `docs/architecture.md`
- 코드 스타일, 커밋/브랜치/PR 규칙 -> `docs/conventions.md`
- controller, dto 작업 -> `.claude/rules/api-layer.md` (자동 적용)
- exception, entity 작업 -> `.claude/rules/exception-entity.md` (자동 적용)
- git 작업 전 -> `.claude/rules/git-workflow.md`
- PR 단위 작업을 문서로 정리할 때 -> `feature-doc` 스킬 (형식은 `docs/features/_template.md`)
- 변경사항을 객관적으로 검토받고 싶을 때 -> `reviewer` 서브에이전트
- AI가 과거에 틀렸던 것 -> `.dev/learnings/`

## 주요 명령어
```bash
./gradlew build      # 컴파일 + 테스트 (contextLoads는 DB 연결이 필요)
./gradlew test       # 테스트만
./gradlew bootRun    # 로컬 실행 (profile: local)
```
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 작업 방식
- 작업 전에 이해한 내용을 먼저 요약하고, 모호한 점은 질문으로 해소한 뒤 진행한다.
- 큰 작업은 계획 -> 승인 -> 실행 -> 검증 순서로 한다. 시작 전에 측정 가능한 완료 조건을 적는다.
- 새 기능은 기존 패턴을 재사용한다: 응답은 `ResponseUtils`, 예외는 `Backend14thException(ErrorCode.X)`, 엔티티는 `BaseEntity` 상속.
- 요청 범위 밖의 파일은 수정하지 않는다. 필요해 보이면 먼저 제안한다.
- 완료라고 말하기 전에 `./gradlew build`로 확인한다. 실행하지 못했다면 그렇다고 말한다.
- 훅을 수정하면 차단돼야 할 명령과 통과돼야 할 명령을 모두 테스트하고, 기대 exit code와 비교한 결과를 보고한다.
- 같은 실수를 반복하면 `.dev/learnings/`에 기록하고 rule로 승격을 제안한다.
- 작업 메모와 handoff는 `.dev/scratchpad/`에 둔다 (Git에 올리지 않음).

## 영구 금지
- `.env` 파일을 읽거나, 수정하거나, 출력하지 않는다. (settings.json deny + `.claude/hooks/block-env-access.sh` 훅으로 차단)
- `git push --force`, `git reset --hard`, `git clean -f`, `git branch -D` 등 되돌리기 어려운 git 명령을 쓰지 않는다. (훅으로 차단)
- `develop`, `seungmin/basic`에 직접 커밋하지 않는다. 작업 브랜치에서만 커밋한다.
- 비밀번호, 토큰 같은 민감 정보를 코드나 yml에 하드코딩하지 않는다.
