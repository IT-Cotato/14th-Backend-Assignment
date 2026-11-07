#!/bin/bash
INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command')

if echo "$COMMAND" | grep -Eq 'git[[:space:]]+push[[:space:]]+.*(--force|-f)'; then
  echo "Blocked: force push는 허용되지 않아요" >&2
  exit 2
fi

if echo "$COMMAND" | grep -Eq 'git[[:space:]]+push[[:space:]].*[[:space:]]develop([[:space:]]|$)'; then
  echo "Blocked: develop에 직접 push하지 않아요" >&2
  exit 2
fi

exit 0