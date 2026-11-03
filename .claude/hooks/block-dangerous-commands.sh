#!/bin/bash

# Claude Code가 전달하는 JSON 입력을 읽는다.
INPUT=$(cat)

# Bash 도구에서 실행하려는 명령어를 추출한다.
COMMAND=$(printf '%s' "$INPUT" | jq -r '.tool_input.command // ""')

# 위험한 명령어 패턴 검사
if [[ "$COMMAND" =~ rm[[:space:]]+-rf ]] || \
   [[ "$COMMAND" =~ git[[:space:]]+push[[:space:]]+.*--force ]] || \
   [[ "$COMMAND" =~ git[[:space:]]+reset[[:space:]]+--hard ]]; then

    echo "차단: 위험한 명령어가 감지되었습니다: $COMMAND" >&2
    exit 2
fi

# 문제가 없으면 실행을 계속한다.
exit 0