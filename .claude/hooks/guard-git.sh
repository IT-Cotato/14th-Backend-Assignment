#!/bin/bash
# PreToolUse(Bash) 훅: 되돌리기 어려운 git 명령을 실행 전에 막는다.
# - 보호 브랜치(develop, */basic)로 직접 push
# - force push
# - .env를 git add

INPUT=$(cat)

# 명령어는 JSON 맨 위가 아니라 tool_input.command 안에 있다.
if command -v jq >/dev/null 2>&1; then
  CMD=$(printf '%s' "$INPUT" | jq -r '.tool_input.command // empty')
else
  # jq가 없을 때를 위한 대체 파싱
  CMD=$(printf '%s' "$INPUT" | sed -n 's/.*"command"[[:space:]]*:[[:space:]]*"\(\([^"\\]\|\\.\)*\)".*/\1/p')
fi

[ -z "$CMD" ] && exit 0

block() {
  echo "Blocked: $1" >&2
  exit 2
}

# git push 검사
if printf '%s' "$CMD" | grep -qE '(^|[;&| ])git push'; then
  # force push
  if printf '%s' "$CMD" | grep -qE 'git push.*(--force|--force-with-lease|[[:space:]]-f([[:space:]]|$))'; then
    block "force push는 허용되지 않아요."
  fi

  # 보호 브랜치를 명시한 push (예: git push origin develop, git push origin HEAD:develop)
  if printf '%s' "$CMD" | grep -qE 'git push.*[[:space:]:](develop|[A-Za-z0-9_-]+/basic)([[:space:]]|$|[;&|])'; then
    block "develop, 이름/basic 브랜치에는 직접 push할 수 없어요. 작업 브랜치에 push한 뒤 PR을 올려주세요."
  fi

  # 브랜치를 지정하지 않은 push는 현재 브랜치를 확인
  if printf '%s' "$CMD" | grep -qE 'git push([[:space:]]+(-u|--set-upstream))?([[:space:]]+origin)?[[:space:]]*($|[;&|])'; then
    CURRENT=$(git -C "${CLAUDE_PROJECT_DIR:-.}" rev-parse --abbrev-ref HEAD 2>/dev/null)
    if printf '%s' "$CURRENT" | grep -qE '^(develop|[A-Za-z0-9_-]+/basic)$'; then
      block "현재 브랜치($CURRENT)는 보호 브랜치라 직접 push할 수 없어요."
    fi
  fi
fi

# .env를 git add
if printf '%s' "$CMD" | grep -qE 'git add[^;&|]*(^|[[:space:]/])\.env([[:space:]]|$|[;&|])'; then
  block ".env는 Git에 올리면 안 돼요. DB 비밀번호 같은 민감 정보가 들어 있어요."
fi

exit 0
