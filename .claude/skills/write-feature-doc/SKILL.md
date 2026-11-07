---
name: write-feature-doc
description: 현재 브랜치의 커밋과 변경 파일을 근거로 docs/features/에 PR 제출용 작업 정리 문서를 작성합니다. PR을 제출하기 전이나 이번 작업 정리 문서를 요청받았을 때 사용합니다.
---

# 작업 정리 문서 작성

## 참고 문서
- 형식 예시: `docs/features/week4-harness.md`
- Git 규칙: `.claude/rules/git-workflow.md`
- 민감 정보 규칙: `.claude/rules/secrets.md`
- 코드 규칙과 구조: `docs/conventions.md`, `docs/architecture.md`
- PR 형식: `.github/PULL_REQUEST_TEMPLATE.md`

## 절차
1. 변경 내용을 수집합니다.
   - `git log --oneline minho/basic..HEAD`
   - `git diff --stat minho/basic...HEAD`
   - `git status --short`
2. 변경된 파일의 diff를 읽습니다. `.env`는 열지 않습니다.
3. `docs/features/`에서 이번 작업의 문서를 찾습니다. 있으면 그 문서를 보완하고, 없으면 새 파일을 계획합니다.
4. 파일 이름은 `week{주차}-{작업명}.md` 형식으로 정합니다. 주차를 알 수 없으면 사용자에게 묻습니다.
5. 작성할 내용의 개요를 사용자에게 보여 주고 승인을 받습니다.
6. 다음 순서로 작성합니다.
   - `## 목표`: 이번 작업의 목적
   - `## 변경 사항`: 변경 내용과 관련 커밋 해시
   - `## 검증`: 실행한 명령과 결과
   - `## 한계와 미확인 사항`: 제약과 확인하지 못한 내용
   - `## 남은 과제`: 이번 PR에서 처리하지 않은 작업
7. 작성한 내용을 대조합니다.
   - 파일 경로와 클래스 이름이 실제로 존재하는지 확인합니다.
   - 커밋 해시가 `git log` 결과에 있는지 확인합니다.
   - 비밀번호, 토큰, 접속 정보 값이 없는지 확인합니다.
8. 작성하거나 수정한 파일과 대조 결과를 보고합니다.

## 작성 규칙
- 코드, diff, 실행 결과로 확인한 내용만 씁니다.
- 실행하지 않은 검증은 `## 검증`에 쓰지 않고 `## 한계와 미확인 사항`에 씁니다.
- 커밋, 푸시, PR 생성은 사용자가 요청할 때만 합니다.
