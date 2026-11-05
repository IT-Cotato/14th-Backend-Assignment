# Git 규칙

- 커밋 메시지는 `타입: 요약` 형식으로 쓴다. 타입은 feat, fix, docs, style, refactor, chore 중 하나다.
- 요약은 한글로, 무엇을 했는지 구체적으로 쓴다. ("수정", "fix" 같은 메시지 금지)
- 한 커밋에는 한 가지 목적만 담는다. 성격이 다른 변경은 커밋을 나눈다.
- 커밋 전에 `git status`로 `.env`, `build/`, `.idea/` 같은 파일이 포함되지 않았는지 확인한다.
- `git add .` 대신 변경한 파일을 명시해서 add한다.
- 사용자가 요청하지 않으면 commit, push를 하지 않는다.
- PR 제목과 본문 형식은 `docs/conventions.md`를 따른다.
