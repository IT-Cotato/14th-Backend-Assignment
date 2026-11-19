#!/bin/bash
INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command')

# git push --force / -f 차단 (브랜치명에 들어간 "-f"는 통과: 예) hoonki/fix-frontend)
if echo "$COMMAND" | grep -qE "git push.*(--force|[[:space:]]-f([[:space:]]|$))"; then
  echo "Blocked: force push는 허용되지 않습니다." >&2
  exit 2
fi

# .env 파일 git add 차단 (루트·backend/·frontend/ 어디에 있든). .env.example은 허용
if echo "$COMMAND" | grep -qE "git add"; then
  if echo "$COMMAND" | grep -oE "[^[:space:]]*\.env[^[:space:]]*" | grep -vqE "\.env\.example$"; then
    echo "Blocked: .env 파일은 git에 추가할 수 없습니다. (backend/.env, frontend/.env 포함)" >&2
    exit 2
  fi
fi

exit 0
