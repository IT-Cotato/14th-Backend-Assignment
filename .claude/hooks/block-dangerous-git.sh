#!/bin/bash
# 위험한 git push를 실행 "전에" 막는 훅 (PreToolUse, Bash|PowerShell)
# - git push --force / -f  : 원격 브랜치의 커밋 기록을 덮어써서 지울 수 있어요
# - main, develop에 직접 push : 1주차 규칙상 금지예요. 작업 브랜치에서 PR로 올려요
#
# Windows에는 jq가 없을 수 있어서, jq 없이 받은 JSON 전체에서 패턴을 찾아요.

INPUT=$(cat)

# 1) 강제 push 막기 (--force, --force-with-lease, -f)
if echo "$INPUT" | grep -qE 'git[^"]*push[^"]*(--force|[[:space:]]-f([[:space:]]|"|$))'; then
  echo "Blocked: git push --force는 허용되지 않아요. 원격의 커밋 기록을 지울 수 있어요." >&2
  exit 2
fi

# 2) main, develop 브랜치에 직접 push 막기
if echo "$INPUT" | grep -qE 'git[^"]*push[^"]*[[:space:]](origin[[:space:]]+)?(main|develop)([[:space:]]|"|$)'; then
  echo "Blocked: main, develop에는 직접 push하지 않아요. 작업 브랜치에 push하고 PR을 올려요." >&2
  exit 2
fi

exit 0
