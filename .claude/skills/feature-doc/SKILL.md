---
name: feature-doc
description: PR 단위 작업 내용을 docs/features/<이슈번호>-<작업명>.md 기능 문서로 정리한다. 기능 구현을 마치고 PR을 올리기 전, 또는 사용자가 "기능 문서", "작업 정리", "PR 정리 문서"를 요청할 때 사용. 인자 예시 - /feature-doc 7 book-crud
---

# 기능 문서 작성

## 절차
1. 이슈번호와 작업명을 정한다.
   - `$ARGUMENTS`가 있으면 `<이슈번호> <작업명>`으로 파싱한다.
   - 없으면 `git branch --show-current`에서 추출한다. (예: `feat/7-book-crud` -> `7`, `book-crud`)
   - 그래도 모르면 사용자에게 묻는다. 추측하지 않는다.
2. 변경 범위를 파악한다.
   - `git log --oneline <base>..HEAD`, `git diff --stat <base>...HEAD`, `git diff <base>...HEAD`
   - base는 과제 브랜치면 `seungmin/basic`, 그 외는 `develop`
3. `docs/features/_template.md`를 읽고 같은 섹션 순서로 `docs/features/<이슈번호>-<작업명>.md`를 만든다.
   - 파일이 이미 있으면 덮어쓰지 말고 바뀐 부분만 갱신한다.
4. 섹션별 작성 기준
   - 배경: 이슈 내용이나 사용자 설명 기준. 근거가 없으면 사용자에게 묻는다.
   - 구현 내용: 추가/수정된 주요 클래스와 요청 흐름 (controller -> service -> repository)
   - API 명세: 컨트롤러의 매핑과 DTO, `ErrorCode`를 실제 코드에서 읽어 표로 쓴다.
   - 설계 결정: diff나 대화에서 확인된 것만 쓴다.
   - 검증: 실제로 실행한 명령과 결과만 쓴다. (예: `./gradlew build` 성공)
   - 남은 이슈: TODO, 미구현 항목
5. 확인할 수 없는 값(PR 링크, 검증 결과 등)은 `(TODO: 확인 필요)`로 남기고, 마지막에 사용자에게 목록으로 알려준다.
6. 작성일은 오늘 날짜(YYYY-MM-DD)로 쓴다.

## 하지 말 것
- 소스 코드를 수정하지 않는다. 문서만 쓴다.
- 코드에 없는 API, 에러 케이스, 검증 결과를 지어내지 않는다.
- 커밋하지 않는다. 커밋은 사용자가 요청할 때 `.claude/rules/git-workflow.md`를 따른다.
