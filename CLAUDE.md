# CLAUDE.md

## 프로젝트 개요
- Cotato 14기 백엔드 과제 레포입니다.
- Java + Gradle 프로젝트이고, 소스 루트는 `src/` 입니다.
- 베이스 패키지는 `(여기에 ls로 확인한 패키지)` 입니다.
- 앱 실행/테스트는 Gradle 래퍼(`./gradlew`)를 사용합니다.

## 어디서 무엇을 읽을지
- 전체 컨벤션, 명령어, 금지 사항 → `CLAUDE.md`
- 폴더 구조 → `docs/project-structure.md`
- 브랜치/이슈/PR → `docs/git-workflow.md`
- 전체 작업 원칙 → `.claude/rules/general.md`
- `src/` 자바 파일 수정 시 → `.claude/rules/java-src.md`
- 여러 단계 작업 절차 → Skills

## 주요 명령어
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 실행: `./gradlew bootRun`
- 테스트 없이 빌드: `./gradlew build -x test`

Windows면 `gradlew.bat`을 씁니다. 맥이니 `./gradlew`를 씁니다.

## 작업 방식
- 작업 브랜치는 `daeun/harness`입니다. `develop`에 직접 커밋하지 않습니다.
- PR은 `daeun/harness` → `daeun/basic` 으로 올립니다.
- 코드를 바꾸기 전에 관련 파일과 `docs/`, Rules를 먼저 읽습니다.
- 큰 절차는 Skills를 따르고, CLAUDE.md에는 원칙만 둡니다.
- 변경은 작게 커밋하고, 커밋 타입은 작업 성격에 맞게 고릅니다. (`feat`, `chore`, `docs`)

## 영구 금지
- `develop`에 직접 푸시하지 않는다.
- GitHub 비밀번호로 git 인증하지 않는다. 토큰 또는 `gh auth login`을 쓴다.
- `.git` 폴더 안에서 작업하지 않는다. 프로젝트 루트에서 작업한다.
- CLAUDE.md를 200줄 넘게 늘리지 않는다. 길어지면 Rules/docs/Skills로 옮긴다.
- 과제 범위를 넘는 리팩터, 의존성 추가, 포맷터 대량 변경을 하지 않는다.

- 주간 과제 커밋/이슈/PR 절차 → `.claude/skills/submit-pr/SKILL.md` (`/submit-pr`)