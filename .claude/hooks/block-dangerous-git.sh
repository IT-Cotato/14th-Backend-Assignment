#!/bin/bash
# PreToolUse(Bash) 훅: 되돌리기 어려운 git 명령(push --force, reset --hard)을 막는다.

input=$(cat)
command=$(printf '%s' "$input" | python3 -c 'import sys, json; print(json.load(sys.stdin).get("tool_input", {}).get("command", ""))')

PUSH_FORCE='git[^;&|]*[[:space:]]push[^;&|]*[[:space:]](-f|--force)([[:space:]]|$)'
RESET_HARD='git[^;&|]*[[:space:]]reset[^;&|]*[[:space:]]--hard([[:space:]]|$)'

if printf '%s' "$command" | grep -Eq "$PUSH_FORCE"; then
  echo "차단: git push --force는 승인 없이 실행할 수 없습니다. 필요하면 사용자에게 먼저 확인하세요." >&2
  exit 2
fi

if printf '%s' "$command" | grep -Eq "$RESET_HARD"; then
  echo "차단: git reset --hard는 작업 내용을 지울 수 있어 승인 없이 실행할 수 없습니다. 필요하면 사용자에게 먼저 확인하세요." >&2
  exit 2
fi

exit 0
