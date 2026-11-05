# Git 규칙

- 커밋 메시지는 `타입: 요약` 형식으로 쓴다. (예: `feat: Book CRUD API 구현`)
- `.env` 파일을 절대 `git add`하지 않는다. 커밋 전 `git status`로 확인한다.
- `git push --force`, `git push -f`를 사용하지 않는다.
- `main`, `develop` 브랜치에 직접 커밋하거나 push하지 않는다.
- 커밋 전 변경 파일 목록을 확인하고, 의도하지 않은 파일이 포함되지 않았는지 검토한다.
