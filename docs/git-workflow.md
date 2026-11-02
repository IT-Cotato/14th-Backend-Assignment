# Git 작업 절차

## 브랜치

`minho/basic`에서 `minho/{작업명}` 브랜치를 만들어 작업합니다.

```powershell
git switch minho/basic
git pull
git switch -c minho/{작업명}
```

## 커밋

| 접두어 | 용도 |
|---|---|
| `feat:` | 기능 추가 |
| `fix:` | 버그 수정 |
| `chore:` | 설정, 빌드 등 기타 작업 |
| `docs:` | 문서 변경 |

## PR

- base는 반드시 `minho/basic`입니다.
- `.github/PULL_REQUEST_TEMPLATE.md` 형식으로 작성합니다.

```powershell
gh pr create --base minho/basic
```

## 금지 사항

- `develop`으로 PR을 보내지 않습니다.
- `develop`에 직접 커밋하거나 푸시하지 않습니다.
- 강제 푸시를 하지 않습니다.

Claude Code에서는 `.claude/hooks/guard-git-target.ps1`이 이 규칙의 주요 위반을 차단합니다. 터미널에서 직접 실행하는 Git 명령에는 적용되지 않습니다.
