#!/bin/bash
INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command')

# git push --force / -f 차단
if echo "$COMMAND" | grep -qE "git push.*(--force|-f)"; then
  echo "Blocked: force push는 허용되지 않습니다." >&2
  exit 2
fi

# .env 파일 git add 차단
if echo "$COMMAND" | grep -qE "git add.*(\.env|\.env\.)"; then
  echo "Blocked: .env 파일은 git에 추가할 수 없습니다." >&2
  exit 2
fi

exit 0
