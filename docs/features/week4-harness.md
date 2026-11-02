# 4주차 하네스 작업 정리

## 목표

1~3주차 결과물이 제대로 동작하는지 점검하고, Claude Code가 프로젝트 규칙에 맞게 작업하도록 문서, 규칙, 스킬, 훅을 구성합니다.

## 프로젝트 점검 결과

- `.\gradlew.bat clean build`가 성공했고, 테스트 1건(`contextLoads`)이 통과했습니다.
- `/hello`, `/books/{id}`(정상, 404), Swagger UI, `/v3/api-docs`의 HTTP 응답을 확인했습니다.
- `POST /books` 호출과 Swagger의 브라우저 화면은 확인하지 않았습니다.

## 변경 사항

### 로컬 포트 변경

8080을 다른 Docker 컨테이너가 사용하고 있어 `local` 프로필의 기본 포트를 8081로 바꿨습니다. (`8e1459b`)

### 하네스 구성

| 위치 | 내용 |
|---|---|
| `CLAUDE.md` | 프로젝트 지도, 영구 금지 사항, 문서 링크 |
| `docs/` | 아키텍처, 컨벤션, 데이터베이스, Git 작업 절차 |
| `.claude/rules/` | 전역: Git 작업(`git-workflow`), 민감 정보(`secrets`) / 조건부: 컨트롤러(`api`), 엔티티(`entity`) |
| `.claude/skills/` | 도메인 API 추가(`add-domain-api`), PR 전 점검(`pre-pr-check`), 작업 정리 문서 작성(`write-feature-doc`) |
| `.claude/hooks/` | `.env` 접근 차단, `develop` 대상 Git 작업 차단 |
| `.claude/settings.json` | 훅 연결, `.env` 접근 거부 권한 |
| `.dev/` | AI 작업 기록 폴더 (`learnings`, `scratchpad`) |

## 훅 검증

샘플 입력 28건으로 차단과 허용을 시험했고 모두 기대한 결과가 나왔습니다. 실제 `.env`는 열지 않았습니다.

Claude Code 세션에서도 `.env` 차단 훅이 실제로 실행되는 것을 확인했습니다. 샘플 문자열 `.env`가 들어 있던 테스트 명령이 차단되었습니다.

## 훅의 한계

- 터미널에서 직접 실행하는 명령에는 적용되지 않습니다.
- 명령 문자열만 검사하므로 다른 스크립트 안에서 `.env`를 읽는 경우는 막지 못합니다.
- `git switch develop && git commit`처럼 한 명령 안에서 브랜치를 바꾸면 전환 전 브랜치를 기준으로 판단합니다.
- 명령에 샘플 문자열로 `.env`가 들어가도 차단될 수 있습니다.
- `pwsh`가 없는 환경에서는 훅이 실행되지 않습니다.

## 남은 과제

- 테스트 DB 분리: 테스트가 실제 로컬 DB를 사용하고 `ddl-auto: update`도 적용됩니다.
- springdoc 버전 확인: 현재 2.8.5이며, Spring Boot 4.1.1과의 호환성 확인이 필요합니다.
- `CorsConfig` 확인: 허용 목록에 `localhost:8081`이 없습니다.
- `/hello` 응답 확인: 응답에 다른 이름("강철웅")이 들어 있습니다.
- `HelloController` 위치 확인: `domain/`, `global/` 밖인 패키지 루트에 있습니다.
