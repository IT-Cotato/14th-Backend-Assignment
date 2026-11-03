# 4주차 하네스 구축

이번 PR에서 만든 것을 정리한 문서입니다.

## 무엇을 만들었는지
- `CLAUDE.md` : 개요, 명령어, 작업 방식, 금지 사항
- `docs/project-structure.md` : 공통 구조/컨벤션
- `docs/git-workflow.md` : 브랜치/이슈/PR 규칙
- `.claude/rules/general.md` : 전체 규칙
- `.claude/rules/java-src.md` : 자바 소스에만 적용되는 규칙

## 왜 만들었는지
- 긴 규칙을 CLAUDE.md 하나에 넣지 않으려고 역할을 나눴습니다.
- 공통 문서는 `docs/`에, 이번 제출 설명은 `docs/features/`에 둡니다.
- 자바 규칙은 `paths:`로 `src/**/*.java`에만 적용합니다.