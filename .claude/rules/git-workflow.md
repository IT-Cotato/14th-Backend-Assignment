# Git 작업 규칙

## 브랜치
- `minho/basic`에서 `minho/{작업명}` 브랜치를 만들어 작업합니다.

## 커밋
- 메시지는 `feat:`, `fix:`, `chore:`, `docs:` 중 하나로 시작합니다.
- 사용자가 요청할 때만 커밋하고 푸시합니다.
- 강제 푸시를 하지 않습니다.

## PR
- base는 반드시 `minho/basic`입니다.
- `.github/PULL_REQUEST_TEMPLATE.md` 형식으로 작성합니다.

## develop 금지
- `develop`으로 PR을 보내지 않습니다.
- `develop`에 직접 커밋하거나 푸시하지 않습니다.

## 코드 수정 전 계획
- 코드를 수정하기 전에 바꿀 파일과 변경 내용을 사용자에게 보여 줍니다.
- 사용자가 승인한 뒤에 수정합니다.
