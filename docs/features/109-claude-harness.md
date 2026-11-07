# Claude Code 하네스 설정

- 이슈: #109
- PR:
- 작성일: 2026-11-05

## 배경
4주차 과제. Claude Code로 작업할 때 프로젝트 규칙(컨벤션, 계층 구조, 금지 사항)을 따르도록 하네스를 구성한다.

## 구현 내용
애플리케이션 코드 변경은 없고, Claude Code 설정 파일만 추가했다.

| 파일 | 역할 |
| --- | --- |
| `CLAUDE.md` | 프로젝트 개요, 문서 위치 안내, 주요 명령어, 작업 방식, 영구 금지 사항 |
| `.claude/settings.json` | gradle `compileJava`/`build`/`test` 허용, `.env` Read/Edit/Write 거부, PreToolUse(Bash) 훅 등록 |
| `.claude/hooks/guard-git.sh` | Bash 명령 실행 전 검사. 아래 경우를 exit 2로 차단 |
| `.claude/rules/controller.md` | `src/main/java/**/controller/**` 작업 시 적용되는 컨트롤러 규칙 (응답 형식, 구조, 예외, 검증, Swagger) |
| `.claude/rules/git.md` | 커밋 메시지 형식, 커밋 단위, add 방식, commit/push는 요청 시에만 |
| `.claude/skills/feature-doc/` | PR 기능 문서 작성 스킬과 템플릿 (`references/template.md`) |

### guard-git.sh 차단 대상
- force push (`--force`, `--force-with-lease`, `-f`)
- 보호 브랜치(`develop`, `*/basic`)를 지정한 push (예: `git push origin develop`, `HEAD:develop`)
- 브랜치를 지정하지 않은 push인데 현재 브랜치가 보호 브랜치인 경우
- `git add`에 `.env`가 포함된 경우

명령어는 훅 입력 JSON의 `tool_input.command`에서 읽는다. `jq`가 없으면 sed로 대체 파싱한다.

## 설계 결정
`CLAUDE.md`의 "영구 금지" 항목 중 되돌리기 어려운 git 작업(보호 브랜치 push, force push, `.env` 커밋)을 훅으로 실행 전에 차단하고, `.env` 접근은 `permissions.deny`로 막는다. 컨트롤러 규칙은 해당 경로 작업 시에만 적용되도록 `rules/`에 분리했다. 자세한 규칙과 이유는 `CLAUDE.md`를 따른다.

## 검증
Claude Code 세션에서 직접 실행해 훅 동작을 확인했다.

| 명령 | 결과 |
| --- | --- |
| `git push --dry-run origin develop` | 차단: "develop, 이름/basic 브랜치에는 직접 push할 수 없어요. 작업 브랜치에 push한 뒤 PR을 올려주세요." |
| `git push --dry-run -f origin soohyun/harness` | 차단: "force push는 허용되지 않아요." |

`--dry-run`이 붙어 있어도 훅이 먼저 차단한다.

## 남은 이슈
