# Git 작업 규칙

- 커밋 메시지는 `<type>: <한글 설명>` 형식으로 쓴다. 자세한 규칙은 `docs/conventions.md`의 Git 절.
- 커밋 전에 메시지 초안과 포함할 파일 목록을 사용자에게 보여주고 승인을 받는다.
- `git add .`이나 `git add -A`를 쓰지 않는다. 파일을 명시해서 스테이징한다.
- 스테이징 후 `git status`로 `.env`, `local/`, `.dev/scratchpad/`, `build/`가 포함되지 않았는지 확인한다.
- `develop`, `seungmin/basic`에서는 커밋하지 않는다. 현재 브랜치가 이 둘이면 멈추고 사용자에게 알린다.
- `git push --force`, `git reset --hard`, `git clean -f`, `git branch -D` 같은 되돌리기 어려운 명령은 쓰지 않는다. (훅으로 차단)
- 이슈/PR 본문 초안은 `.dev/scratchpad/`에 만든다.
