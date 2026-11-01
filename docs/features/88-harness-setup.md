# Claude Code 하네스 설정

- 이슈: #88
- PR:
- 작성일: 2026-11-01

## 배경
4주차 과제 요구사항이기도 하지만, 실제 목적은 AI가 git을 직접 건드리지 못하게 하고 요청 범위 밖의 파일을 수정하지 않게 하는 것이다.
이전 프로젝트에서 브랜치와 커밋을 관리하다가 실수가 많았다. 그래서 git 작업만큼은 사람이 전부 통제하도록 했다.

## 구현 내용
Claude Code가 이 저장소에서 따를 지침, 규칙, 훅, 스킬, 참고 문서를 만들었다.

| 파일 | 역할 |
|---|---|
| `CLAUDE.md` | 프로젝트 개요(Spring Boot 4.1.1 / Java 21, 베이스 패키지, 연습용 `Book` 도메인), 패키지 구조, 주요 명령어, 작업 방식, 영구 금지 사항(`.env` 읽기 금지, 모든 git 명령 금지, Spring Boot·Java 버전 변경 금지) |
| `.claude/rules/work-principles.md` | 항상 적용되는 작업 원칙. 한국어 응답, 수정 전 설명, 요청 범위 밖 파일 수정 금지, 파일 생성·삭제 전 확인, 에러 원인을 마지막 `Caused by:`까지 확인, 라이브러리 버전 변경 제안 금지, 검증 방법 안내 |
| `.claude/rules/controller.md` | `src/main/java/**/controller/**` 파일을 다룰 때만 적용되는 규칙. 필수 어노테이션, `@PathVariable` 이름 명시, `ResponseEntity<ApiResponse<...>>` + `ResponseUtils` 응답, `Backend14thException(ErrorCode.XXX)` 사용, Controller 안 try-catch와 비즈니스 로직 금지 |
| `.claude/settings.json` | Bash 도구를 실행하기 전에 `block-git.ps1`을 부르도록 `PreToolUse` 훅 등록 |
| `.claude/hooks/block-git.ps1` | 훅 입력 JSON의 `tool_input.command`를 읽는다. 명령이 `git`으로 시작하면 종료 코드 2로 차단하고, 명령을 직접 실행하지 말고 사용자에게 알려주라는 메시지를 돌려준다. JSON을 해석하지 못하면 차단하고, 명령이 비어 있으면 통과시킨다 |
| `.claude/skills/write-feature-doc/SKILL.md` | 작업한 기능을 `docs/features/<이슈번호>-<작업명>.md` 문서로 정리하는 절차와 형식 |
| `docs/conventions.md` | 코딩 컨벤션. 도메인별 패키지 구조, 클래스·DTO 이름 규칙, `BaseEntity` 상속과 ID 생성 전략, 요청 DTO의 Lombok 어노테이션, Controller 응답·Swagger 어노테이션 |
| `docs/error-handling.md` | 예외 처리 가이드. `Backend14thException` → `GlobalExceptionHandler` → `ErrorResponse` 흐름, `ErrorCode` 추가 방법과 `도메인-번호` 코드 형식, `RuntimeException`을 직접 던지면 500이 되는 이유 |

git 차단 흐름: Claude가 Bash 명령을 실행하려 함 → `PreToolUse` 훅이 `block-git.ps1` 실행 → git 명령이면 차단 메시지 반환, 아니면 통과

## API 명세
해당 없음 (API를 추가하거나 바꾸지 않음)

## 설계 결정
- **CLAUDE.md만 쓸지, 훅도 쓸지**: CLAUDE.md는 부탁일 뿐이라 AI가 읽고도 실행할 수 있다. 그래서 훅으로 한 번 더 막았다. 테스트해 보니 첫 시도에서는 AI가 CLAUDE.md를 읽고 스스로 거부했고, 강제로 시도하게 하자 훅이 막았다.
- **bash + jq로 쓸지, PowerShell로 쓸지**: 워크북 예시는 bash와 jq를 쓰지만 Windows PowerShell 환경에는 둘 다 없어서 PowerShell 스크립트로 작성했다. WSL에서 실행하는 방법도 검토했지만, Node와 Claude Code를 다시 설치해야 하고 경로에 한글과 공백이 있어서 포기했다.
- **git 전체를 막을지, push만 막을지**: CLAUDE.md에 모든 git 명령 금지라고 적었으니 훅도 같은 범위로 맞췄다.
- **차단 메시지 언어**: Claude가 읽는 메시지라서 인코딩 때문에 글자가 깨지지 않도록 영어로 썼다. 스크립트 첫 줄에서 출력 인코딩도 UTF-8로 맞췄다.
- **명령 검사 방식**: 정규식 `^git(\s|$)`를 쓴다. `git status`와 `git` 단독 입력은 막고, `gitignore`처럼 git으로 시작하는 다른 단어는 통과시킨다.
- **JSON 해석 실패 시**: 명령을 확인할 수 없을 때는 통과시키지 않고 막는 쪽을 택했다.
- **Controller 규칙 범위**: `paths`를 지정해서 controller 파일을 다룰 때만 읽히게 했다.

## 검증
- `/hooks` 명령으로 훅이 `PreToolUse:Bash`에 등록된 것을 확인했다.
- 2026-11-01에 Claude에게 Bash 도구로 `git status`를 실행하게 했다. 훅이 `Blocked: git commands must be run by the user. Suggest the command instead of running it.` 메시지를 돌려주며 차단했고, 명령은 실행되지 않았다.
- `ls` 명령을 실행시켜 git이 아닌 명령은 정상적으로 통과하는 것을 확인했다.
- `/` 메뉴에서 `write-feature-doc` 스킬이 목록에 뜨는 것을 확인했다.
- 이 문서는 `write-feature-doc` 스킬을 실행해서 작성했다.

## 남은 이슈
- 훅 matcher가 `Bash`뿐이라 PowerShell 도구로 실행하는 git 명령은 막지 못한다. (아직 시험하지 않음)
- 명령의 맨 앞만 검사하므로 `cd x && git status`, `ls; git status`처럼 git이 중간에 오는 명령은 통과된다. (코드만 보고 판단했고, 직접 시험하지는 않음)
- PowerShell 스크립트라서 macOS나 Linux 환경에서는 동작하지 않는다. 팀원과 공유하려면 다른 방식이 필요하다.
- 규칙과 스킬이 실제 개발 중에 잘 동작하는지는 5주차에 도메인 개발을 하면서 확인해야 한다.
